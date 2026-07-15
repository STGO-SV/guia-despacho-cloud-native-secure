const ROLE_LABELS = {
  GESTION_GUIAS: "Instructor",
  INSTRUCTOR: "Instructor",
  DESCARGA_GUIAS: "Estudiante",
  ESTUDIANTE: "Estudiante"
};

export function detectRole(claims = {}, claimName = "extension_RolGuia") {
  const raw = claims[claimName] ?? claims.roles ?? [];
  const roles = Array.isArray(raw) ? raw : [raw];
  for (const role of roles.map(value => String(value).trim().toUpperCase())) {
    if (ROLE_LABELS[role]) return { code: role, label: ROLE_LABELS[role] };
  }
  return { code: "SIN_ROL", label: "Sin rol reconocido" };
}

export function buildAuthorizationHeader(accessToken) {
  if (!accessToken || !String(accessToken).trim()) throw new Error("No hay access token disponible");
  return { Authorization: `Bearer ${String(accessToken).trim()}` };
}

export function describeHttpError(status) {
  if (status === 401) return "401 · Sesión ausente o token no válido. Inicia sesión nuevamente.";
  if (status === 403) return "403 · La sesión es válida, pero el rol no autoriza esta operación.";
  return `${status} · La solicitud no pudo completarse.`;
}
