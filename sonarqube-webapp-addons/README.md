# sq-server-addons

This library reimplements removed features from the SonarQube Community Build frontend to support the community branch plugin.

Most code is refactored from the [`SonarSource/sonarqube-webapp@2025.1.0.10869`](https://github.com/SonarSource/sonarqube-webapp/tree/2025.1.0.10869).

### Setup

```bash
# Clone git submodule
git submodule update --init --recursive

# Create symlink for local development
./sonarqube-webapp-addons/setup.sh

cd sonarqube-webapp

# Install dependencies
yarn
```

### Local Development

By default, the UI targets SonarQube at http://localhost:9000. You can override this by setting the `PROXY` environment variable.

```bash
PROXY=http://my-sonarqube.org yarn start-sqs
```

### Building

If you are building locally, run the [setup script](#setup) first to create the symlink. The Docker build overwrites the directory instead.

```bash
yarn nx run sq-server:build
```

The distribution files are generated in the `sonarqube-webapp/apps/sq-server/build/webapp' directory.

### Testing

There are currently no tests for this project; however, it should be possible to refactor these from the original codebase.
