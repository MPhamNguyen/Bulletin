const fs = require("node:fs");
const path = require("node:path");

const projectRoot = path.resolve(__dirname, "..");
const seedDataDirectory = path.join(__dirname, "seedData");

loadDotEnv(path.join(projectRoot, ".env"));

const supabaseUrl = process.env.SUPABASE_URL?.replace(/\/$/, "");
const apiKey = process.env.SUPABASE_API_KEY || process.env.SUPABASE_ANON_KEY;

if (!supabaseUrl) {
    throw new Error("SUPABASE_URL is required.");
}

if (!apiKey) {
    throw new Error("SUPABASE_API_KEY or SUPABASE_ANON_KEY is required.");
}

const profiles = readCsv(path.join(seedDataDirectory, "Profiles_SeedData.csv"))
    .filter((row) => row.email && row.full_name);
const reviews = readCsv(path.join(seedDataDirectory, "Review_SeedData.csv"))
    .filter((row) => row.reviewer_id && row.reviewee_id && row.comment);
const listings = readCsv(path.join(seedDataDirectory, "Listing_SeedData.csv"))
    .filter((row) => row.name && row.description);

if (profiles.length === 0) {
    throw new Error("Profiles_SeedData.csv does not contain any usable profiles.");
}

async function main() {
    for (const profile of profiles) {
        const email = profile.email.trim().toLowerCase();
        if (!isUuid(profile.id)) {
            throw new Error(
                `Profile ${email} must contain the UUID of its manually created Supabase Auth user in the id column.`
            );
        }

        profile.authUserId = profile.id.trim();
        console.log(`Using Auth UUID ${profile.authUserId} for ${email}`);
    }

    const profileRows = profiles.map((profile) => ({
        id: profile.authUserId,
        email: profile.email.trim().toLowerCase(),
        full_name: profile.full_name.trim(),
        major: (profile.major || "").trim(),
        university: (profile.university || "").trim(),
        bio: (profile.bio || "").trim(),
        is_verified: parseBoolean(profile.is_verified)
    }));

    await supabaseRequest("/rest/v1/profiles?on_conflict=id", {
        method: "POST",
        headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
        body: profileRows
    });
    console.log(`Upserted ${profileRows.length} profiles.`);

    const profileIdsByEmail = new Map(
        profiles.map((profile) => [profile.email.trim().toLowerCase(), profile.authUserId])
    );

    const reviewRows = reviews.map((review, index) => ({
        reviewer_id: resolveProfileId(profileIdsByEmail, review.reviewer_id),
        reviewee_id: resolveProfileId(profileIdsByEmail, review.reviewee_id),
        score: parseInteger(review.score, "score"),
        comment: review.comment.trim(),
        created_at: parseCreatedAt(review, index)
    }));

    for (const review of reviewRows) {
        if (review.score < 1 || review.score > 5) {
            throw new Error(`Review score must be between 1 and 5: ${review.score}`);
        }
    }

    if (reviewRows.length > 0) {
        await supabaseRequest("/rest/v1/reviews", {
            method: "POST",
            body: reviewRows
        });
        console.log(`Inserted ${reviewRows.length} reviews.`);
    }

    const listingRows = listings.map((listing, index) => ({
        name: listing.name.trim(),
        user_id: profileRows[index % profileRows.length].id,
        category: listing.category.trim(),
        condition: listing.condition.trim(),
        price: listing.price.trim() === "" ? null : Number(listing.price),
        description: listing.description.trim(),
        created_at: listing.created_at.trim() || new Date().toISOString()
    }));

    for (const listing of listingRows) {
        if (listing.price !== null && !Number.isFinite(listing.price)) {
            throw new Error(`Listing price is not a number: ${listing.price}`);
        }
    }

    if (listingRows.length > 0) {
        await supabaseRequest("/rest/v1/listings", {
            method: "POST",
            body: listingRows
        });
        console.log(`Inserted ${listingRows.length} listings across ${profileRows.length} profiles.`);
    }

    console.log("Seed completed.");
}

async function supabaseRequest(endpoint, options = {}) {
    const response = await fetch(`${supabaseUrl}${endpoint}`, {
        method: options.method || "GET",
        headers: {
            apikey: apiKey,
            Authorization: `Bearer ${apiKey}`,
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });

    const responseText = await response.text();
    let responseBody = null;

    if (responseText) {
        try {
            responseBody = JSON.parse(responseText);
        } catch {
            responseBody = responseText;
        }
    }

    if (!response.ok) {
        throw new Error(`${options.method || "GET"} ${endpoint} failed (${response.status}): ${formatError(responseBody)}`);
    }

    return responseBody;
}

function isUuid(value) {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value.trim());
}

function resolveProfileId(profileIdsByEmail, value) {
    const email = value.trim().toLowerCase();
    const id = profileIdsByEmail.get(email);

    if (!id) {
        throw new Error(`Review references an email that is not in Profiles_SeedData.csv: ${value}`);
    }

    return id;
}

function parseCreatedAt(row, index) {
    if (row.created_at && row.created_at.trim()) {
        const timestamp = Date.parse(row.created_at);
        if (Number.isNaN(timestamp)) {
            throw new Error(`created_at must be a valid timestamp: ${row.created_at}`);
        }
        return new Date(timestamp).toISOString();
    }

    if (row.created_at_millis && row.created_at_millis.trim()) {
        const timestamp = parseInteger(row.created_at_millis, "created_at_millis");
        return new Date(timestamp).toISOString();
    }

    return new Date(Date.UTC(2026, 8, 14, 12, 0, index)).toISOString();
}

function parseInteger(value, fieldName) {
    const parsed = Number(value);
    if (!Number.isInteger(parsed)) {
        throw new Error(`${fieldName} must be an integer: ${value}`);
    }
    return parsed;
}

function parseBoolean(value) {
    return ["true", "1", "yes", "y"].includes(String(value).trim().toLowerCase());
}

function formatError(value) {
    if (typeof value === "string") {
        return value;
    }
    return JSON.stringify(value);
}

function readCsv(filePath) {
    const content = fs.readFileSync(filePath, "utf8").replace(/^\uFEFF/, "");
    const rows = parseCsv(content);
    const [headers, ...dataRows] = rows;

    if (!headers) {
        return [];
    }

    return dataRows
        .filter((row) => row.some((value) => value.trim() !== ""))
        .map((row) => Object.fromEntries(headers.map((header, index) => [header.trim(), row[index] || ""])));
}

function parseCsv(content) {
    const rows = [];
    let row = [];
    let field = "";
    let quoted = false;

    for (let index = 0; index < content.length; index += 1) {
        const character = content[index];

        if (character === '"') {
            if (quoted && content[index + 1] === '"') {
                field += '"';
                index += 1;
            } else {
                quoted = !quoted;
            }
        } else if (character === "," && !quoted) {
            row.push(field);
            field = "";
        } else if ((character === "\n" || character === "\r") && !quoted) {
            if (character === "\r" && content[index + 1] === "\n") {
                index += 1;
            }
            row.push(field);
            rows.push(row);
            row = [];
            field = "";
        } else {
            field += character;
        }
    }

    if (field || row.length > 0) {
        row.push(field);
        rows.push(row);
    }

    return rows;
}

function loadDotEnv(filePath) {
    if (!fs.existsSync(filePath)) {
        return;
    }

    for (const line of fs.readFileSync(filePath, "utf8").split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith("#")) {
            continue;
        }

        const separator = trimmed.indexOf("=");
        if (separator === -1) {
            continue;
        }

        const key = trimmed.slice(0, separator).trim();
        const value = trimmed.slice(separator + 1).trim().replace(/^['"]|['"]$/g, "");
        if (!process.env[key]) {
            process.env[key] = value;
        }
    }
}

main().catch((error) => {
    console.error(`Seed failed: ${error.message}`);
    process.exitCode = 1;
});
