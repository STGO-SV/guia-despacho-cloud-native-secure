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

function text(value) {
  const candidate = Array.isArray(value) ? value[0] : value;
  if (candidate == null) return "";
  const normalized = String(candidate).trim();
  return ["unknown", "undefined", "—"].includes(normalized.toLowerCase()) ? "" : normalized;
}

export function displayUserName(claims = {}, account = {}) {
  const fullName = [text(claims.given_name), text(claims.family_name)].filter(Boolean).join(" ");
  const candidates = [
    claims.name,
    fullName,
    claims.preferred_username,
    claims.email,
    claims.emails,
    account.name,
    account.username
  ];
  return candidates.map(text).find(Boolean) || "Usuario autenticado";
}

export function sessionView(account, claimName = "extension_RolGuia") {
  const authenticated = Boolean(account);
  const role = authenticated
    ? detectRole(account.idTokenClaims ?? {}, claimName)
    : { code: "SIN_ROL", label: "—" };
  return {
    authenticated,
    role,
    userName: authenticated ? displayUserName(account.idTokenClaims ?? {}, account) : "—",
    showLogin: !authenticated,
    showLogout: authenticated,
    showInstructorPanel: authenticated && role.label === "Instructor",
    showStudentPanel: authenticated && role.label === "Estudiante",
    showAuthorizationEvidence: authenticated
  };
}

export function buildEnrollmentPayload(cursoId) {
  return { cursoId: Number(cursoId) };
}

export function buildAuthorizationHeader(accessToken) {
  if (!accessToken || !String(accessToken).trim()) throw new Error("No hay access token disponible");
  return { Authorization: `Bearer ${String(accessToken).trim()}` };
}

export function describeHttpError(status) {
  if (status === 401) return "401 · Sesión ausente o token no válido. Inicia sesión nuevamente.";
  if (status === 403) return "403 · La sesión es válida, pero el rol no autoriza esta operación.";
  if (status === 409) return "409 · Ya estás inscrito en este curso.";
  return `${status} · La solicitud no pudo completarse.`;
}
