"use strict";

const accountAction = document.getElementById("action");
const suspensionFields = document.getElementById("suspension-duration");
const durationInput = document.getElementById("duration");
const durationUnit = document.getElementById("duration-unit");

if (accountAction && suspensionFields && durationInput && durationUnit) {
  const updateDuration = () => {
    const suspending = accountAction.value === "suspend";
    suspensionFields.hidden = !suspending;
    suspensionFields.disabled = !suspending;
    durationInput.max = {days: 365, hours: 8760, minutes: 525600}[durationUnit.value];
  };
  accountAction.addEventListener("change", updateDuration);
  durationUnit.addEventListener("change", updateDuration);
  window.addEventListener("pageshow", updateDuration);
  updateDuration();
}
