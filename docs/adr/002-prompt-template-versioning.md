# ADR 002: Version-Control Prompt Templates with Compatibility Rules

## Status

Accepted

## Context

Prompt templates change frequently as teams iterate on LLM behavior. Without governance, a change to a prompt template can silently break downstream agents that depend on specific variables being present.

## Decision

Store prompt templates as versioned artifacts in Apicurio Registry with BACKWARD compatibility rules enabled.

## Rationale

- BACKWARD compatibility ensures callers of the previous version can use the new one. Apicurio
  Registry rejects a new version (HTTP 400) when:
  - a variable is removed but the template still uses it
  - a variable's type changes
  - an optional variable becomes required
  - an allowed (enum) value of a variable is removed
  - the output schema, or one of its properties, is removed
- Adding variables, required or not, is accepted. Defaults are not checked, so give a new
  variable a default if existing callers won't send it.
- Version history enables rollback if a new prompt degrades quality
- Teams can pin to specific versions and migrate gradually

## Consequences

- All prompt template changes must go through the registry
- Breaking changes require explicit version bumps and coordinated migration
- Variables must be designed with forward-thinking defaults
- The talk demos in this repository do not use prompt templates; see the Apicurio Registry agent
  registry demo for a runnable example
