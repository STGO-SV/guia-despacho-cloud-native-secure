import { describe, expect, it } from "vitest";
import {
  buildAuthorizationHeader,
  buildEnrollmentPayload,
  describeHttpError,
  detectRole,
  displayUserName,
  sessionView
} from "./auth-utils.js";

describe("utilidades de autenticación", () => {
  it("mapea los roles antiguos y nuevos", () => {
    expect(detectRole({ extension_RolGuia: "GESTION_GUIAS" }).label).toBe("Instructor");
    expect(detectRole({ extension_RolGuia: ["ESTUDIANTE"] }).label).toBe("Estudiante");
    expect(detectRole({ extension_RolGuia: ["desconocido", "DESCARGA_GUIAS"] }).code)
      .toBe("DESCARGA_GUIAS");
  });

  it("construye Authorization sin registrar el token", () => {
    expect(buildAuthorizationHeader("token-demo")).toEqual({ Authorization: "Bearer token-demo" });
  });

  it("explica 401", () => expect(describeHttpError(401)).toContain("Inicia sesión"));
  it("explica 403", () => expect(describeHttpError(403)).toContain("rol"));
  it("explica 409 sin presentarlo como JSON inválido", () => {
    expect(describeHttpError(409)).toBe("409 · Ya estás inscrito en este curso.");
  });

  it("usa una cadena robusta para el nombre visible", () => {
    expect(displayUserName({ given_name: "Ana", family_name: "Pérez" })).toBe("Ana Pérez");
    expect(displayUserName({ emails: ["ana@example.test"] })).toBe("ana@example.test");
    expect(displayUserName({}, { username: "cuenta@example.test" })).toBe("cuenta@example.test");
    expect(displayUserName({ name: "unknown", preferred_username: "alumna@example.test" }))
      .toBe("alumna@example.test");
    expect(displayUserName({ name: " Unknown ", email: "correo@example.test" }))
      .toBe("correo@example.test");
    expect(displayUserName()).toBe("Usuario autenticado");
  });

  it("muestra solo el panel correspondiente y alterna login/logout", () => {
    const signedOut = sessionView(null);
    expect(signedOut).toMatchObject({ showLogin: true, showLogout: false,
      showInstructorPanel: false, showStudentPanel: false, showAuthorizationEvidence: false });

    const instructor = sessionView({ idTokenClaims: { extension_RolGuia: "INSTRUCTOR", name: "Docente" } });
    expect(instructor).toMatchObject({ showLogin: false, showLogout: true,
      showInstructorPanel: true, showStudentPanel: false, showAuthorizationEvidence: true,
      userName: "Docente" });

    const student = sessionView({ idTokenClaims: { extension_RolGuia: "ESTUDIANTE" }, username: "alumno" });
    expect(student).toMatchObject({ showLogin: false, showLogout: true,
      showInstructorPanel: false, showStudentPanel: true, showAuthorizationEvidence: true,
      userName: "alumno" });
  });

  it("construye una inscripción que contiene únicamente cursoId numérico", () => {
    expect(buildEnrollmentPayload("7")).toEqual({ cursoId: 7 });
    expect(JSON.stringify(buildEnrollmentPayload("7"))).toBe('{"cursoId":7}');
  });
});
