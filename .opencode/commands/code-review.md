---
description: Read-only review of the current changes by the reviewer agent
agent: reviewer
---

Review the current changes in this repository. $ARGUMENTS

Start from `git status` and `git diff`, then read the affected code and its call
sites. Report findings by severity with `file:line`, why each one matters, the
concrete fix, and finish with a verdict.
