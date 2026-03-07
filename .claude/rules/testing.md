---
description: Testing conventions and patterns
globs:
  - "**/*Test.kt"
  - "**/*test*.kt"
---

# Testing

- Fakes over mocks — reusable test doubles, not inline mocking
- Real code paths — test through actual components/functions, fake only I/O boundaries
- Test pyramid — many unit tests, some integration, minimal instrumented tests
- Write modular code — pure functions are easy to test, push I/O to edges
- Verify semantically correct behavior
- Failing tests are acceptable when they expose genuine bugs and test correct behavior
