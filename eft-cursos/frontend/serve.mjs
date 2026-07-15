import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize, resolve, sep } from "node:path";

const root = resolve("dist");
const mime = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".css": "text/css; charset=utf-8" };

createServer((request, response) => {
  const pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
  const candidate = normalize(join(root, pathname === "/" ? "index.html" : pathname));
  const insideRoot = candidate === root || candidate.startsWith(`${root}${sep}`);
  const file = insideRoot && existsSync(candidate) && statSync(candidate).isFile()
    ? candidate : join(root, "index.html");
  response.writeHead(200, { "Content-Type": mime[extname(file)] ?? "application/octet-stream", "Cache-Control": "no-store" });
  createReadStream(file).pipe(response);
}).listen(5173, "0.0.0.0", () => console.log("Frontend EFT disponible en http://localhost:5173"));
