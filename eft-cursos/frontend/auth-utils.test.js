import { describe, expect, it } from "vitest";
import { buildAuthorizationHeader, describeHttpError, detectRole } from "./auth-utils.js";

describe("utilidades de autenticación", () => {
  it("mapea los roles antiguos y nuevos", () => {
    expect(detectRole({ extension_RolGuia: "GESTION_GUIAS" }).label).toBe("Instructor");
    expect(detectRole({ extension_RolGuia: ["ESTUDIANTE"] }).label).toBe("Estudiante");
  });

  it("construye Authorization sin registrar el token", () => {
    expect(buildAuthorizationHeader("token-demo")).toEqual({ Authorization: "Bearer token-demo" });
  });

  it("explica 401", () => expect(describeHttpError(401)).toContain("Inicia sesión"));
  it("explica 403", () => expect(describeHttpError(403)).toContain("rol"));
});
