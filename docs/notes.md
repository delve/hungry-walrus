# Learning
## excessive testing
The developer agent write tests to validate the behavior of some base Java (like, java.time) and 3rd party packages and had to be corrected. I had to tell it to check that tests were all testing our code. And explicitly to update its handoff to reflect the test removals. It's POSSIBLE the codereview agent would have caught it, and if i see this behavior again i'll check to see. but if one agent was able to make this mistake a review agent is equally likely. maybe a note in its definition to verify tests only cover our own code would help there.

I am not certain if that was the developer agent doing it, of a 'base' claude agent. i typed the prompts into the same claude panel used to invoke the pipeline slash commands.
`Why are the tests verifying the behavior of a base Java package?` - java.time test in open in focus file removed
`Review other tests to ensure we are only testing the behavior of our own code.` additional tests removed
`update your handoff to note the deleted tests and why.` handoff updated.

## permissions
developer needs DELETE, or else it can't clean up code files that should be removed and leaves stubs behind.

## code review
include verbiage about identifying findings to improve reference stability. "consistently label findings as C## for critical, W## for warnings, or O## for observations which may not need remediation."

Include explicit instruction to ignore deferred findings. Not sure how to handle deferrence where the fix agent was wrong. perhaps manual update to the initial review saying don't defer, then rerun fix?
/pipeline:codereview 03  "ui layer" Where findings from previous sessions were deferred consider the rationale and it it is sound do not re-raise the same finding.

# Agent notes
i have 6 agents
architect, codereview, designer, developer, devops, and qa

multiple slash commands can invoke the same agent. i tend to use the agent name and the command name interchangably.

the developer agent seems to work best with a clean session, the codereview agent seems to work best reusing the same session

# App notes
## initial manual pipeline
You → Architect → You → Designer → You → Developer → Code Review Agent → You → QA → You → Developer (if fixes needed) → You → DevOps → You.

And your pipeline execution order is:

1. Architect — produces architecture.md `/pipeline:architect`
1. You review
1. Design — produces design.md `/pipeline:design`
1. You review
1. Developer session 00 — project scaffolding `/pipeline:scaffold`
1. Developer session 01 — data layer `/pipeline:develop 01  "data layer"`
1. Start a new session. Code Review session 01 — reviews data layer `/pipeline:codereview 01  "data layer"`
1. You review, send fixes back to Developer if needed - OPTIONAL: Start a new session. Run fix for review findings `/pipeline:fix 01  "data layer"` then repeat previous step.
1. Developer session 02 — domain layer `/pipeline:develop 02  "domain layer"`
1. Start a new session. Code Review session 02 — reviews that layer `/pipeline:codereview 02  "domain layer"`
1. You review, send fixes back to Developer if needed - OPTIONAL: Start a new session. Run fix for review findings `/pipeline:fix 02  "domain layer"` then repeat previous step.
1. Developer session 03 — UI layer `/pipeline:develop 03  "ui layer"`
1. Start a new session. Code Review session 03 — reviews UI layer `/pipeline:codereview 03  "ui layer"`
1. You review, send fixes back to Developer if needed - OPTIONAL: Start a new session. Run fix for review findings `/pipeline:fix 03  "ui layer"` then repeat previous step.
1. QA — integration tests, additional unit tests, full report `/pipeline:test`
1. You review, send bugs back to Developer if needed
1. DevOps — CI/CD pipeline, signing docs, build commands
1. You review

# TODOs
TODO: As per 8.4 in design.md, add serving size to the food cache