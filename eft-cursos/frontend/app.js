import {
  BrowserCacheLocation,
  InteractionRequiredAuthError,
  PublicClientApplication
} from "@azure/msal-browser";
import { buildAuthorizationHeader, buildEnrollmentPayload, describeHttpError, sessionView } from "./auth-utils.js";

const config = window.EFT_CONFIG ?? {};
const required = ["B2C_CLIENT_ID", "B2C_AUTHORITY", "B2C_KNOWN_AUTHORITY", "B2C_REDIRECT_URI", "B2C_SCOPE"];
const missing = required.filter(name => !config[name]);
const scopes = config.B2C_SCOPE ? [config.B2C_SCOPE] : [];
const msal = new PublicClientApplication({
  auth: {
    clientId: config.B2C_CLIENT_ID || "00000000-0000-0000-0000-000000000000",
    authority: config.B2C_AUTHORITY,
    knownAuthorities: config.B2C_KNOWN_AUTHORITY ? [config.B2C_KNOWN_AUTHORITY] : [],
    redirectUri: config.B2C_REDIRECT_URI || window.location.origin,
    postLogoutRedirectUri: config.B2C_REDIRECT_URI || window.location.origin
  },
  cache: { cacheLocation: BrowserCacheLocation.SessionStorage, storeAuthStateInCookie: false }
});

const el = id => document.getElementById(id);
const loginButton = el("loginButton");
const logoutButton = el("logoutButton");
const loadCoursesButton = el("loadCoursesButton");
const courseForm = el("courseForm");
const enrollmentForm = el("enrollmentForm");
const enrollmentCourseSelect = el("enrollmentCourseSelect");
const instructorPanel = el("instructorPanel");
const studentPanel = el("studentPanel");
const authorizationEvidence = el("authorizationEvidence");
const forbiddenButton = el("forbiddenButton");
let currentRole = { code: "SIN_ROL", label: "—" };

async function initialize() {
  await msal.initialize();
  const response = await msal.handleRedirectPromise();
  if (response?.account) msal.setActiveAccount(response.account);
  if (!msal.getActiveAccount()) msal.setActiveAccount(msal.getAllAccounts()[0] ?? null);
  if (missing.length) {
    el("configurationNotice").hidden = false;
    el("configurationNotice").textContent = `Configuración B2C pendiente: ${missing.join(", ")}. Completa config.js antes del login real.`;
    loginButton.disabled = true;
  }
  renderSession();
}

function renderSession() {
  const account = msal.getActiveAccount();
  const view = sessionView(account);
  currentRole = view.role;
  el("authStatus").textContent = view.authenticated ? "Autenticado" : "No autenticado";
  el("userName").textContent = view.userName;
  el("roleStatus").textContent = currentRole.label;
  loginButton.hidden = !view.showLogin;
  loginButton.disabled = missing.length > 0;
  logoutButton.hidden = !view.showLogout;
  logoutButton.disabled = !view.authenticated;
  instructorPanel.hidden = !view.showInstructorPanel;
  studentPanel.hidden = !view.showStudentPanel;
  authorizationEvidence.hidden = !view.showAuthorizationEvidence;
  loadCoursesButton.disabled = !view.authenticated;
  courseForm.querySelector("button").disabled = !view.showInstructorPanel;
  enrollmentCourseSelect.disabled = !view.showStudentPanel || enrollmentCourseSelect.options.length <= 1;
  enrollmentForm.querySelector("button").disabled = enrollmentCourseSelect.disabled;
  forbiddenButton.disabled = !view.authenticated || currentRole.code === "SIN_ROL";
  if (!view.authenticated) {
    el("coursesList").innerHTML = '<p class="empty">Inicia sesión para consultar cursos.</p>';
    populateEnrollmentCourses([]);
  }
  el("forbiddenDescription").textContent = currentRole.label === "Estudiante"
    ? "Intentará crear un curso con rol estudiante; el resultado esperado es 403."
    : currentRole.label === "Instructor"
      ? "Intentará crear una inscripción con rol instructor; el resultado esperado es 403."
      : "La acción se habilita después del login.";
}

async function login() {
  const response = await msal.loginPopup({ scopes: ["openid", "offline_access", ...scopes], prompt: "select_account" });
  msal.setActiveAccount(response.account);
  renderSession();
  await loadCourses();
}

async function logout() {
  const account = msal.getActiveAccount();
  await msal.logoutPopup({ account, postLogoutRedirectUri: config.B2C_REDIRECT_URI });
  msal.setActiveAccount(null);
  renderSession();
}

async function accessToken() {
  const account = msal.getActiveAccount();
  if (!account) throw new Error("Debes iniciar sesión");
  try {
    return (await msal.acquireTokenSilent({ account, scopes })).accessToken;
  } catch (error) {
    if (!(error instanceof InteractionRequiredAuthError)) throw error;
    return (await msal.acquireTokenPopup({ account, scopes })).accessToken;
  }
}

async function api(path, options = {}) {
  const token = await accessToken();
  const response = await fetch(`${config.BFF_BASE_URL}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...buildAuthorizationHeader(token), ...options.headers }
  });
  const text = await response.text();
  let body = text;
  try { body = text ? JSON.parse(text) : null; } catch { /* respuesta no JSON */ }
  showResponse(response.status, body, response.ok ? "Solicitud completada" : describeHttpError(response.status));
  if (!response.ok) throw new Error(describeHttpError(response.status));
  return body;
}

function showResponse(status, body, message) {
  el("httpStatus").textContent = status ? `HTTP ${status}` : "Error local";
  el("httpStatus").className = `http-status ${status >= 400 ? "error" : "success"}`;
  el("httpResponse").textContent = `${message}\n\n${typeof body === "string" ? body : JSON.stringify(body, null, 2)}`;
}

async function loadCourses() {
  const courses = await api("/api/bff/cursos");
  const container = el("coursesList");
  container.innerHTML = "";
  if (!courses?.length) container.innerHTML = '<p class="empty">No hay cursos registrados.</p>';
  for (const course of courses ?? []) {
    const article = document.createElement("article");
    article.innerHTML = `<strong></strong><span></span><small></small>`;
    article.querySelector("strong").textContent = course.titulo;
    article.querySelector("span").textContent = course.descripcion;
    article.querySelector("small").textContent = `ID ${course.id} · ${course.estado} · ${course.instructor}`;
    container.append(article);
  }
  populateEnrollmentCourses(courses ?? []);
}

function populateEnrollmentCourses(courses) {
  enrollmentCourseSelect.replaceChildren();
  const prompt = document.createElement("option");
  prompt.value = "";
  prompt.textContent = courses.length ? "Selecciona un curso" : "No hay cursos disponibles";
  enrollmentCourseSelect.append(prompt);
  for (const course of courses) {
    const option = document.createElement("option");
    option.value = String(course.id);
    option.textContent = `ID ${course.id} · ${course.titulo}`;
    enrollmentCourseSelect.append(option);
  }
  const enabled = currentRole.label === "Estudiante" && courses.length > 0;
  enrollmentCourseSelect.disabled = !enabled;
  enrollmentForm.querySelector("button").disabled = !enabled;
}

courseForm.addEventListener("submit", async event => {
  event.preventDefault();
  const body = Object.fromEntries(new FormData(courseForm));
  try { await api("/api/bff/cursos", { method: "POST", body: JSON.stringify(body) }); courseForm.reset(); await loadCourses(); }
  catch (error) { if (!String(error.message).startsWith("4")) showResponse(0, error.message, "Error local"); }
});

enrollmentForm.addEventListener("submit", async event => {
  event.preventDefault();
  const body = buildEnrollmentPayload(new FormData(enrollmentForm).get("cursoId"));
  try { await api("/api/bff/inscripciones", { method: "POST", body: JSON.stringify(body) }); enrollmentForm.reset(); }
  catch (error) { if (!String(error.message).startsWith("4")) showResponse(0, error.message, "Error local"); }
});

forbiddenButton.addEventListener("click", async () => {
  const instructorAttempt = { cursoId: 1 };
  const studentAttempt = { titulo: "Intento prohibido", descripcion: "Debe devolver 403", instructor: "Estudiante", estado: "ACTIVO" };
  const path = currentRole.label === "Estudiante" ? "/api/bff/cursos" : "/api/bff/inscripciones";
  const body = currentRole.label === "Estudiante" ? studentAttempt : instructorAttempt;
  try { await api(path, { method: "POST", body: JSON.stringify(body) }); }
  catch { /* api ya presenta el 403 */ }
});

loginButton.addEventListener("click", () => login().catch(error => showResponse(0, error.message, "Login no completado")));
logoutButton.addEventListener("click", () => logout().catch(error => showResponse(0, error.message, "Logout no completado")));
loadCoursesButton.addEventListener("click", () => loadCourses().catch(error => {
  if (!String(error.message).startsWith("4")) showResponse(0, error.message, "No fue posible cargar cursos");
}));

initialize().catch(error => showResponse(0, error.message, "No fue posible inicializar MSAL"));
