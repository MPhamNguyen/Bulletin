"""Vercel entrypoint. Select admin as the project's Root Directory.

Local: set the variables in .env.example in your shell, then run
python -m flask --app app run. No environment file is loaded automatically.
"""

from portal.web import create_app

app = create_app()
