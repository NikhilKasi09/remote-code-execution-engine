# Remote Code Execution Engine

A web-based code runner: write Python or C++ in a browser editor, hit run, and get back the output of that code executed on a server - safely, inside a locked-down, single-use Docker container.

It's the same basic idea behind LeetCode's or Replit's "run" button: take arbitrary, untrusted user code and execute it without letting it do anything to the machine it's running on.

![demo](docs/demo.mp4)

## How it works

The frontend (React + [Monaco](https://microsoft.github.io/monaco-editor/), the editor component that powers VS Code) sends the submitted code and selected language to a single backend endpoint, `POST /api/execute`. From there:

1. The code is written to a fresh temporary directory, one per request.
2. The backend shells out to `docker run`, mounting that directory into a brand-new container for the selected language (`python:3.9` or `gcc:latest`), with the container locked down as tightly as the language still allows:
   - `--network=none` - no network access at all
   - `--memory=256m --cpus=0.5` - hard resource caps
   - `--pids-limit=64` - blocks fork-bomb style abuse
   - `--read-only` - the container's own filesystem can't be written to
   - `--user 1000:1000` - runs as an unprivileged user, not root
3. Output is redirected straight to a file, capped at 100 KB so a runaway `print` loop can't exhaust server memory.
4. Execution is killed if it runs past a per-language timeout (5s for Python, 10s for C++, since compilation eats into that budget).
5. The temporary directory and the container are both torn down immediately after, whether the run succeeded, failed, or timed out.
6. Requests are rate-limited per IP (5 requests/minute) before any of the above runs, so a single abusive client can't spin up unlimited containers.

## Stack

- **Frontend:** React 19, Vite, Monaco Editor, Axios
- **Backend:** Spring Boot 4, Java 21
- **Execution sandbox:** Docker (invoked directly by the backend via the Docker CLI)
- **Deployment:** GitHub Actions → Docker Hub → AWS EC2

In production, the frontend is built and its static output is embedded directly into the Spring Boot jar, so what actually runs on the server is a single Java process.

## Running it locally

Requires Docker running locally (the backend needs it even in dev, since it's what actually executes the submitted code).

**Backend**
```bash
cd backend/execution-engine
./mvnw spring-boot:run
```
Runs on `localhost:8080`.

**Frontend**
```bash
cd frontend
npm install
npm run dev
```
Runs on Vite's dev server and proxies `/api` requests to the backend.

## API

```
POST /api/execute
Content-Type: application/json

{
  "language": "python",   // or "cpp"
  "code": "print('hello')"
}
```

Response:
```json
{ "output": "hello\n" }
```

If the rate limit is hit, the response is `429 Too Many Requests` with a JSON error body instead.

## Security model - and its limits

This project runs untrusted code, so it's worth being explicit about what's actually protected against and what isn't, rather than leaving it implied.

**What's handled:**
- No network access from inside the sandbox (rules out data exfiltration or the container being used to attack anything else).
- CPU, memory, and process-count limits (rules out most resource-exhaustion attacks).
- A short, enforced execution timeout with a forced container kill on expiry.
- Execution as a non-root, unprivileged container user.
- Basic per-IP rate limiting on the endpoint itself.

**What isn't, and why:**
- The backend runs *inside* its own Docker container, and reaches the host's Docker daemon by mounting `/var/run/docker.sock` (a "Docker-outside-of-Docker" setup) so it can launch sibling containers. Anything with access to that socket has effective root on the host - so if a future change ever let request data influence the Docker invocation, or a container-escape vulnerability were found in the sandboxed runtime, the blast radius extends to the host. A production version of this would replace direct socket access with a scoped [docker-socket-proxy](https://github.com/Tecnativa/docker-socket-proxy), or move sandboxing to something with stronger isolation than shared-kernel containers, like gVisor or Firecracker microVMs (the latter is what services like Fly.io and CodeSandbox actually use).
- There's no user authentication - the API is intentionally open so anyone can try it, with rate limiting as the only abuse control.
- Rate limiting is in-memory (a `ConcurrentHashMap` keyed by client IP), which is correct for a single instance but wouldn't hold up unmodified across multiple backend replicas behind a load balancer.

## What's next

- Additional languages (currently just Python and C++)
- Persisted execution history / shareable snippets
- Replacing direct Docker socket access with a scoped proxy or a stronger-isolation sandbox
- WebSocket-based streaming output instead of waiting for the full result
