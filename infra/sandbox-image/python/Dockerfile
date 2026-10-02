FROM python:3.12-slim

RUN useradd --create-home --uid 10001 runner && mkdir -p /workspace && chown runner /workspace
WORKDIR /workspace
USER runner

ENTRYPOINT ["python"]
