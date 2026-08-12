# Test Harness

This Maven harness compiles the authority Runtime template directly from
`skill/assets/runtime-template/`; it does not maintain a second production source copy.
Test-only mocks/fixtures must never become production Runtime dependencies. Required
coverage is defined in `docs/40-development/41-TEST-AND-ACCEPTANCE.md`.

Run on Windows:

```powershell
mvn.cmd -f test-harness/pom.xml test
```
