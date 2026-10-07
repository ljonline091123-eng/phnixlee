export type ModuleView = "overview" | "data" | "ai" | "research" | "operations";

export type AppRoute = {
  module: ModuleView;
  section: string;
};

export const moduleSections: Record<ModuleView, readonly string[]> = {
  overview: ["status"],
  data: ["access", "master", "business", "lakehouse", "knowledge", "graphs", "pipeline"],
  ai: ["models", "agents", "skills", "evaluation", "governance"],
  research: ["objects", "selection", "research"],
  operations: ["tasks", "quality", "audit"],
};

export const defaultSections: Record<ModuleView, string> = {
  overview: "status",
  data: "access",
  ai: "models",
  research: "objects",
  operations: "tasks",
};

const modules = Object.keys(moduleSections) as ModuleView[];

export function normalizeRoute(moduleValue?: string, sectionValue?: string): AppRoute {
  const module = modules.includes(moduleValue as ModuleView) ? moduleValue as ModuleView : "overview";
  const allowed = moduleSections[module];
  const section = sectionValue && allowed.includes(sectionValue) ? sectionValue : defaultSections[module];
  return { module, section };
}

export function parseRouteHash(hash: string): AppRoute {
  const clean = hash.replace(/^#\/?/, "").split(/[?#]/, 1)[0];
  const [module, section] = clean.split("/").filter(Boolean);
  return normalizeRoute(module, section);
}

export function routeHash(route: AppRoute): string {
  const normalized = normalizeRoute(route.module, route.section);
  return `#/${normalized.module}/${normalized.section}`;
}
