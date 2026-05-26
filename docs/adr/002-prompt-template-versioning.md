# ADR 002: Version-Control Prompt Templates with Compatibility Rules

## Status

Accepted

## Context

Prompt templates change frequently as teams iterate on LLM behavior. Without governance, a change to a prompt template can silently break downstream agents that depend on specific variables being present.

## Decision

Store prompt templates as versioned artifacts in Apicurio Registry with BACKWARD compatibility rules enabled.

## Rationale

- BACKWARD compatibility ensures new versions can be consumed by agents using the previous version
- Adding an optional variable (with a default) is backward compatible
- Removing or renaming a required variable is a breaking change and is rejected
- Version history enables rollback if a new prompt degrades quality
- Teams can pin to specific versions and migrate gradually

## Consequences

- All prompt template changes must go through the registry
- Breaking changes require explicit version bumps and coordinated migration
- Variables must be designed with forward-thinking defaults
