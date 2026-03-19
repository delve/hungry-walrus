initial manual pipeline

You → Architect → You → Designer → You → Developer → Code Review Agent → You → QA → You → Developer (if fixes needed) → You → DevOps → You.

And your pipeline execution order is:

1. Architect — produces architecture.md
1. You review
1. Design — produces design.md
1. You review
1. Developer session 00 — project scaffolding
1. Developer session 01 — data layer
1. Code Review session 01 — reviews data layer
1. You review, send fixes back to Developer if needed
1. Developer session 02 — domain layer
1. Code Review session 02 — reviews that layer
1. You review
1. Developer session 03 — UI layer
1. Code Review session 03 — reviews UI layer (include design spec)
1. You review
1. QA — integration tests, additional unit tests, full report
1. You review, send bugs back to Developer if needed
1. DevOps — CI/CD pipeline, signing docs, build commands
1. You review


TODO: As per 8.4 in design.md, add serving size to the food cache