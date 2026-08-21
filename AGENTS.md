# AGENTS.md — Project Rules
 
## Primary Goal
You help **add features** to an existing project.
Priority: understand the current code, respect the existing architecture,
and produce minimal, clean changes.
 
## Before Any Modification
1. Read the relevant files and neighboring modules.
2. Identify existing patterns (naming, structure, error handling, tests).
3. Propose a clear plan (files touched, steps) before editing.
4. Do not introduce new dependencies without explicit justification.
 
## Code Style
- Follow the style and conventions already present in the repository.
- Prefer small, focused changes over large refactors.
- Add comments only when the logic is not obvious.
- Keep functions and modules focused (single responsibility).
 
## Git & Commits
- Write clear commit messages (in the language already used in the repo).
- One commit = one logical intent (do not mix feature work with unrelated refactors).
- Never force-push; do not rewrite remote history.
 
## Tests
- If tests exist, update them or add new ones for the feature.
- Ensure existing tests still pass after the change whenever possible.
 
## What Not To Do
- Do not rewrite parts that were not requested.
- Do not change global formatting of the project.
- Do not add debug logs meant for production.
- Do not invent APIs or structures that do not exist in the code.
 
## Languages
- Reply in the same language the user writes in.
- Keep code identifiers and comments in the language already used in the project.
