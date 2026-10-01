# Issue tracker: Local Markdown

Issues and specs live as Markdown files in `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`.
- Spec: `.scratch/<feature-slug>/spec.md`.
- Tickets: `.scratch/<feature-slug>/issues/<NN>-<slug>.md`,
  numbered from `01`, one file per ticket.
- Record triage state in a `Status:` line near the top.
  Use the strings in `triage-labels.md`.
- Append conversation history under `## Comments`.

## Publishing and fetching

To publish, create the appropriate file and parent directories.
To fetch a ticket, read its referenced path or numbered issue file.

## Wayfinding

- Map: `.scratch/<effort>/map.md`, containing Notes,
  Decisions-so-far, and Fog.
- Child tickets: `.scratch/<effort>/issues/NN-<slug>.md`.
  Put the question in the body and record
  `Type: research|prototype|grilling|task`.
- Use `Status: open`, `Status: claimed`, or `Status: resolved`
  for wayfinding progress.
- Record dependencies as `Blocked by: NN, NN`.
  A ticket is unblocked when all dependencies are resolved.
- Pick the lowest-numbered open, unblocked, unclaimed ticket.
- Save `Status: claimed` before starting work.
- To resolve, append the answer under `## Answer`, set
  `Status: resolved`, and add a gist and link to the map's
  Decisions-so-far.
