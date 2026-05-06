export const manageOrgApiData = {
  /** Public host — may return HTML 403 to non-browser API clients (Front Door / WAF). */
  defaultBaseUrlAat: 'https://manage-org.aat.platform.hmcts.net',
  /** K8s internal service (AAT). Use when public MO returns 403; requires VPN / cluster network. */
  defaultInternalBaseUrlAat: 'http://xui-mo-webapp-aat.service.core-compute-aat.internal',
};
