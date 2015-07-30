#Gander [![Circle CI](https://circleci.com/gh/intenthq/gander.svg?style=svg)](https://circleci.com/gh/intenthq/gander) [![Coverage Status](https://coveralls.io/repos/intenthq/gander/badge.svg?branch=master&service=github)](https://coveralls.io/github/intenthq/gander?branch=master)

Gander is a scala library that extract content from webpages.
 
It is based on [Goose](https://github.com/GravityLabs/goose) with the idea to:
- Simplify its codebase by removing some of its functionality (like crawling, there are plenty of project that do it well)
- Keep it alive (goose has been inactive for several years now)
- Make its codebase more functional and take advantage of some of newer scala features
