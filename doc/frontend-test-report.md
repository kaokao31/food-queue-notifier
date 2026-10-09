# Frontend workflow verification

Run `node test/js/browser-workflow.test.cjs` and the individual `test/js/*.test.cjs` suites, then `mvn clean test -Dtest=WebMvcIntegrationTest,StaffFormTemplateTest,QrServiceTest,QrControllerTest`.

The workflow test connects the actual basket, checkout, history and queue modules. API responses in `test/fixtures/ui-api.json` are test-only data. It verifies ID/quantity-only submission, one creation request, per-order token storage, history discovery, polling headers, terminal stop and refusal to request an order without its token. MVC mapping tests and actual Thymeleaf rendering verify local asset availability and CSRF-dependent staff forms.

These checks do not establish production authentication, State transitions, subscription or Push delivery. Real browser verification and current screenshots must be captured after those implementations are merged; no previous completed-system screenshots are included as evidence for this stage.

Record native execution results during review. Full end-to-end acceptance remains pending.
