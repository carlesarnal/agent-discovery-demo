# ADR 001: Use Apicurio Registry for Agent Discovery

## Status

Accepted

## Context

AI agents need a mechanism to discover each other's capabilities at runtime. Options considered:

1. **Hardcoded configuration** — Agent URLs and capabilities stored in config files
2. **DNS/Service mesh** — Use existing infrastructure (Consul, Istio) for discovery
3. **A2A well-known endpoints** — Each agent hosts `/.well-known/agent.json`
4. **Registry-backed discovery** — Centralized registry storing Agent Cards

## Decision

Use Apicurio Registry as the centralized store for A2A Agent Cards, prompt templates, and model schemas.

## Rationale

- Apicurio Registry already provides versioning, compatibility checking, and lifecycle management for schemas — these same capabilities apply to AI artifacts
- A2A well-known endpoints require knowing the host first; registry-backed discovery allows querying by capability
- Service meshes discover services, not capabilities — an agent's skills and input/output modes are not expressible in DNS
- Centralized governance enables compatibility rules that prevent breaking changes across the agent ecosystem
- CNCF sandbox project aligns with cloud-native infrastructure

## Consequences

- Agents must register with the registry on startup
- Discovery adds a network hop (registry query) before agent-to-agent communication
- Registry becomes a critical dependency — must be highly available in production
- Agent Cards follow the A2A Protocol format for interoperability
