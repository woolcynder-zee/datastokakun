# GPT Parallel Audit Notes

Review priorities while other tooling works in parallel: backup/restore correctness, Room transaction boundaries, screenshot/file lifecycle, credential handling, App Lock lifecycle, and Compose state correctness.

The latest known CI compile issue was an experimental Material3 API in HomeScreen; commit `7fb04a26b9a5f34a953c4c31758442b4ef5cfa02` adds the required opt-in.

This note is informational only and should not block feature work.
