# Automated Tech Debt Reduction — Task Queue

## Phase 1: Manual Trial (5 backend unit tests)

Target: Execute 5 backend test tasks manually using the orchestrator, validate instruction cards are unambiguous, merge PRs with green CI.

---

## TASK-001 · [backend-test] Add unit tests for FormService.getForms()

> Context: FormService.getForms() currently has no unit test coverage. This is a critical path that retrieves all forms and maps them to DTOs. Risk: low (new test file). Impact: high (frequently used).
> File: backend/src/test/java/com/example/backend/service/FormServiceTest.java
> Source file: backend/src/main/java/com/example/backend/service/FormService.java
> Test command: ./mvnw test -Dtest=FormServiceTest
> Style reference: backend/src/test/java/com/example/backend/controller/FormControllerTest.java

Test scenarios:
1. Happy path: when formRepository returns a list of forms, getForms() maps them to DTOs and returns the list
2. Edge case: when formRepository returns empty list, getForms() returns empty list

- [x] planned
- [ ] branch created · `debt/task-001-add-unit-tests-for`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

---

## TASK-002 · [backend-test] Add unit tests for FormService.getForm() not found error

> Context: FormService.getForm(String) throws NoSuchElementException when form not found. This error path is untested. Risk: low (new test file). Impact: high (error handling on all API reads).
> File: backend/src/test/java/com/example/backend/service/FormServiceErrorTest.java
> Source file: backend/src/main/java/com/example/backend/service/FormService.java
> Test command: ./mvnw test -Dtest=FormServiceErrorTest
> Style reference: backend/src/test/java/com/example/backend/controller/FormControllerTest.java

Test scenarios:
1. Error path: when formRepository.findByFormKey returns empty, getForm() throws NoSuchElementException with message "Form not found: <formKey>"
2. Happy path: when formRepository.findByFormKey returns a form, getForm() returns that form

- [ ] planned
- [ ] branch created · `debt/task-002-add-unit-tests-for`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

---

## TASK-003 · [backend-test] Add unit tests for FormService.saveForm()

> Context: FormService.saveForm() is a transactional method with no unit test coverage. It persists forms and logs the operation. Risk: low (new test file). Impact: high (all form creation operations depend on this).
> File: backend/src/test/java/com/example/backend/service/FormServiceSaveTest.java
> Source file: backend/src/main/java/com/example/backend/service/FormService.java
> Test command: ./mvnw test -Dtest=FormServiceSaveTest
> Style reference: backend/src/test/java/com/example/backend/controller/FormControllerTest.java

Test scenarios:
1. Happy path: when saveForm is called with a form object, formRepository.save is called and the saved form is returned
2. Verify logging: confirm that the service logs "Saving form definition: <formKey>" at info level

- [ ] planned
- [ ] branch created · `debt/task-003-add-unit-tests-for`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

---

## TASK-004 · [backend-test] Add unit tests for FormDataService.saveFormData()

> Context: FormDataService.saveFormData() persists user-submitted form data. No unit test coverage exists. Risk: low (new test file). Impact: high (core business operation, all submissions use this).
> File: backend/src/test/java/com/example/backend/service/FormDataServiceSaveTest.java
> Source file: backend/src/main/java/com/example/backend/service/FormDataService.java
> Test command: ./mvnw test -Dtest=FormDataServiceSaveTest
> Style reference: backend/src/test/java/com/example/backend/controller/FormControllerTest.java

Test scenarios:
1. Happy path: when saveFormData is called with valid FormData, repository.save is called and the saved object is returned
2. Verify that @Transactional ensures the save is committed

- [ ] planned
- [ ] branch created · `debt/task-004-add-unit-tests-for`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

---

## TASK-005 · [backend-test] Add unit tests for ConfigController.getConfig()

> Context: ConfigController.getConfig() returns application configuration. No unit test coverage. Risk: low (new test file). Impact: medium (frontend depends on this for initialization).
> File: backend/src/test/java/com/example/backend/controller/ConfigControllerTest.java
> Source file: backend/src/main/java/com/example/backend/controller/ConfigController.java
> Test command: ./mvnw test -Dtest=ConfigControllerTest
> Style reference: backend/src/test/java/com/example/backend/controller/FormControllerTest.java

Test scenarios:
1. Happy path: GET /config returns 200 with valid config object
2. Happy path: config contains expected fields (e.g., appVersion, apiEndpoint)

- [ ] planned
- [ ] branch created · `debt/task-005-add-unit-tests-for`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->
