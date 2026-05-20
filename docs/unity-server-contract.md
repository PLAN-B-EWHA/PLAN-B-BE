# Unity - Spring API Contract

## Roles

- Web and Unity use the same email/password login API.
- Spring issues JWT tokens and Unity sends the access token with `Authorization: Bearer {token}`.
- The selected playable child is stored on the server per user.
- Unity reads the selected child from the server and starts the game for that child.
- Built-in dialogue scenarios live in Unity.
- LLM scenarios are generated, reviewed, and approved on the web, then served by Spring.
- Unity sends game results to Spring after play ends.

## Scenario Sources

Dialogue results include `scenario_source` so analytics can distinguish Unity built-in content from server-provided content.

Allowed values:

- `UNITY_LOCAL`: built-in Unity scenario.
- `SERVER_LLM`: LLM scenario generated and approved through the web.
- `SERVER_MANUAL`: manually authored server scenario, reserved for future use.

If Unity omits `scenario_source`, Spring stores it as `UNITY_LOCAL` for backward compatibility.

## Scenario Approval

Server scenarios use a small approval state model:

- `DRAFT`: generated or imported draft, not visible to Unity.
- `PUBLISHED`: reviewed and visible to Unity.
- `REJECTED`: reviewed and rejected, not visible to Unity.
- `ARCHIVED`: no longer visible to Unity.

Unity should only fetch server scenarios through:

```text
GET /api/unity/scenarios/published
GET /api/unity/scenarios/published?week=1
```

Only `SERVER_LLM` and `SERVER_MANUAL` scenarios with `approval_status = PUBLISHED` are returned.

Admin or therapist review endpoints:

```text
POST /api/admin/scenarios/{scenarioId}/publish
POST /api/admin/scenarios/{scenarioId}/reject
POST /api/admin/scenarios/{scenarioId}/archive
```

Optional request body:

```json
{
  "review_note": "검수 완료"
}
```

## Unity Login Flow

```text
POST /api/auth/login
-> access token, refresh token

GET /api/game-player/selected-child
Authorization: Bearer {accessToken}
-> selected playable child
```

Unity can use the clearer alias:

```text
GET /api/unity/selected-child
PUT /api/unity/selected-child
Authorization: Bearer {accessToken}
```

PUT body:

```json
{
  "childId": "00000000-0000-0000-0000-000000000000"
}
```

## Dialogue Result

```http
POST /api/unity/game-results/dialogue
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "scenario_id": "W01_SY_001",
  "scenario_source": "UNITY_LOCAL",
  "theme": "정보 교환하기",
  "started_at": "2026-03-03T15:02:00Z",
  "ended_at": "2026-03-03T15:08:30Z",
  "total_score": 4,
  "max_score": 6,
  "turns": [
    {
      "turn_id": 1,
      "selected_option_order": 2,
      "selected_score": 2
    }
  ]
}
```

Notes:

- `child_id` is not required in the request body because Spring resolves the child from the logged-in user and selected playable child.
- `week_number` is not required for result saving. It can be inferred from `scenario_id` or scenario metadata when needed.
- `scenario_source` is currently needed only for dialogue results.
- When `scenario_source` is `SERVER_LLM` or `SERVER_MANUAL`, Spring requires the `scenario_id` to exist as a published server scenario.

## Expression Result

Expression results do not need a source field yet because they are keyed by `emotion_target`, not a dialogue scenario.

```http
POST /api/unity/game-results/expression
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "emotion_target": "sad",
  "started_at": "2026-03-03T15:20:00Z",
  "ended_at": "2026-03-03T15:22:40Z",
  "final_accuracy": 0.81,
  "is_success": true,
  "tries": [
    {
      "try_number": 1,
      "duration_ms": 8200,
      "accuracy_score": 0.42,
      "is_success": false
    }
  ]
}
```

## Realtime API

Unity should not store provider API keys. If GPT Realtime is added, Unity should request a short-lived realtime session from Spring using its JWT. Spring should create the provider session and return only the short-lived client secret/session data to Unity.
