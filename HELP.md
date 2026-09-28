# Studyroom

Studyroom is a Spring Boot tutoring-center app. The current increment provides
secure sign-in, an environment-initialized teacher account, teacher-managed
batches and student accounts, and batch-specific PDF/text note and video
uploads, multiple-choice quizzes with automatic scoring, and private
student-to-teacher messages.

## Run locally

Java 17 or newer is required.

The app now uses PostgreSQL. Copy `.env.example` to `.env`, replace the
placeholder database password with a private password, and start PostgreSQL
and the app:

```sh
cp .env.example .env
docker compose up -d postgres
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Open <http://localhost:8080> and create the teacher account username, recovery
email, and password on the local first-run screen.
The setup screen is only available from the same computer as the running app.
If a teacher account already exists, opening `http://localhost:8080/setup`
lets you replace its login after confirmation, while keeping classroom data.
Alternatively,
`APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` (12+ characters), and
`APP_ADMIN_EMAIL` (recovery address) can initialize the first account from the
environment. Sign in with the teacher username and
password, create a batch, then create student accounts from the
Students page. Give each student their credentials privately. Upload PDF/text
notes or MP4/WebM videos from the learning library and share each item with a
batch. Notes must contain selectable text; scanned-image OCR is not supported.
If an older teacher account has no recovery email yet, set `APP_ADMIN_EMAIL`
to its verified email address and `APP_ADMIN_USERNAME` to that account's
username, then restart once; the app fills only a missing email and never
overwrites an existing recovery address. This does not require resetting the
account or deleting classroom data.

The PostgreSQL database and uploaded files are stored separately: PostgreSQL
uses the persistent Docker volume `studyroom-postgres-data`, and uploads
default to `./data/uploads`. Set `APP_UPLOAD_DIR` to choose another uploads
directory.

Switching to PostgreSQL does not delete the old local H2 file at
`./data/studyroom.mv.db`; PostgreSQL starts with a fresh schema, so old H2
classroom records are not automatically copied. Keep the H2 file and uploads
as a backup. If you need the old records in PostgreSQL, migrate them before
using the new database with real student data.

For an externally managed PostgreSQL server, set `DATABASE_URL`,
`DATABASE_USERNAME`, and `DATABASE_PASSWORD` instead of using Docker Compose.
This local configuration is for development; use managed database backups,
HTTPS, and protected credentials before exposing the app publicly.

Video uploads are limited to 500 MB; PDF and text notes are limited to 20 MB.
Uploads are checked by extension, media type, and file signature where
applicable. Notes are extracted as UTF-8 text for student viewing and possible
future AI processing.

## Current boundaries

Teacher and student routes are role-protected; passwords are BCrypt-hashed and
form submissions use CSRF protection. Public student registration is disabled.
Teacher password reset uses a six-digit code sent to the signup recovery email;
codes expire after 10 minutes and allow at most five attempts. Email sending
requires SMTP configuration. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`,
`MAIL_PASSWORD`, and `MAIL_FROM` in the environment; use an SMTP app password
rather than your normal mailbox password. The code is stored hashed and is
never shown in the browser or application logs.

For example, for an SMTP provider that supports STARTTLS:

```sh
export MAIL_HOST='smtp.example.com'
export MAIL_PORT=587
export MAIL_USERNAME='your-mailbox@example.com'
export MAIL_PASSWORD='your-smtp-app-password'
export MAIL_FROM='your-mailbox@example.com'
./mvnw spring-boot:run
```
Quiz scores are calculated on the server. Student material downloads and quiz
access are restricted to their assigned batch; private conversations are
scoped to one student and the teacher.

AI summaries, suggested tags, and doubt answering are disabled until
`GEMINI_API_KEY` is configured. The app sends note text and student questions
to Google when a key is set. Use only an AI Studio project on Google's free
tier with billing disabled if you want to avoid provider charges; the app cannot inspect or
control the billing status of the Google project behind an API key. No
alternate or paid provider fallback is implemented. `AI_MONTHLY_REQUEST_LIMIT`
defaults to 100 model attempts per calendar month; each
summary generation and each answered doubt consumes one request, and usage is
reserved before the network call so concurrent requests cannot exceed the
configured application cap. Provider-side quota limits may be lower and can
change independently.

To enable them, create a Google AI Studio API key using a project with billing
disabled, then set the variables before startup:

```sh
export GEMINI_API_KEY='your-private-ai-studio-key'
export AI_MONTHLY_REQUEST_LIMIT=100
./mvnw spring-boot:run
```

The key is read only from the server environment and sent in the request
header; it is not placed in browser code or URLs. Students are told in the
doubt-helper UI that their question and the matching note excerpts are sent to
Google when this integration is enabled.

Summaries are generated only on a teacher action for notes up to 24,000
extracted characters, in English and Hindi, with
suggested topic tags. They remain drafts until the teacher reviews and approves
them for students; the teacher can edit, clear, or regenerate them. Doubt answers
retrieve only matching notes from the signed-in student's batch, cite source
notes, and abstain when retrieval or the model cannot support an answer. No
real student data should be used until production storage, backups, HTTPS, and
account recovery are configured.

Run the test suite with `./mvnw test`.
