import branches from './branches';

// Stub exports for commercial SonarQube features not provided by community plugin
// These prevent TypeScript errors in the main SonarQube app

// Create stub components that accept any props
const stubComponent = (_props?: any) => null;
const stubElement = null as any;

export const addons = {
  branches,
  sca: {
    ScaReportOverviewOptions: stubComponent,
    getReleasesUrl: (_component?: any, _branch?: any) => ({ pathname: '', search: '' }),
    PROJECT_LICENSE_ROUTE_NAME: '',
    LICENSE_ROUTE_NAME: '',
    projectRoutes: () => null,
    licenseRoutes: () => stubElement,
  },
  license: undefined,
  architecture: {
    spotlight: (_arg?: any) => null,
    ArchitectureAdminBanner: (_props?: any) => null,
    ArchitectureUserBanner: (_props?: any) => null,
    ArchitectureEnablementForm: stubComponent,
    routes: () => stubElement,
  },
  aica: {
    AICA_SETTINGS_PATH: '',
    aicaSettingsRoutes: () => stubElement,
    getAICodeSettingsUrl: (_component?: any, _profile?: any) => '',
    isQualityProfileRecommendedForAI: (_profile?: any) => false,
    ProfileRecommendedForAiIcon: stubComponent,
    ProfileAICodeSuggestionBanner: stubComponent,
  },
  jira: {
    IssueJiraWorkItem: stubComponent,
    JiraTicketStatusFacet: stubComponent,
    JiraProjectBinding: stubComponent,
    InstanceJiraBinding: stubComponent,
  },
  slack: {
    SlackIntegrationConfiguration: stubComponent,
  },
  issueSandbox: {
    SandboxSettingContainer: stubComponent,
  },
};
