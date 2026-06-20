# AI Coder / Agent Instructions

This project is a highly fictionalized, satirical, and self-contained **Clinical Sim Engine**. 
When writing code, expanding lore, adding actions, or generating copy, you **MUST STICK STRICTLY** to the following foundational rules dictated by the user.

## 1. No Real World Affiliations or Context
- **Fictional Geopolitics Only:** The simulation operates in a fictional sovereign state (e.g., "The Sovereign Republic", "Elysium"). **Do NOT** use real-world geopolitical contexts like "South Africa", "United States", "UK", etc.
- **Universal Terminology:** The currency is strictly the universal Dollar (`$`) or generic "Credits". We actively removed all references to "R" or "ZAR" (South African Rand). Never introduce national currencies, actual political parties, or specific real-world hospital names (like "Chris Hani Baragwanath" or "Johns Hopkins").
- **Generic Medical Acts:** Use fictionalized or highly generalized terminology for medical laws: "National Health Authority", "Supreme Medical Court", "Sovereign Health Act".

## 2. Emphasize the Dystopian/Bureaucratic Satire
- The game loop revolves around a bureaucratic, hyper-capitalist, and sometimes dystopian healthcare system.
- Mechanics like "Sub-Rosa Jury Settlements" (bribes), "Dictatorship Executions" for laws, and "Agentic Overlord" states are central to the identity of the sim.
- Feel free to lean into the oppressive clinical regulatory atmosphere: audits, massive penalties, political lobbying, and chaotic AI events.

## 3. UI/UX and Polish Standards
- M3 dynamic color schemes and high contrast, polished, data-dense interfaces are required.
- Everything runs via local state (`SimulationViewModel.kt`, Room DB, DataStore) augmented by pure LLM REST API calls. 
- Do not introduce real backend infrastructure (Firebase, REST databases). Everything must remain functional as a single-player offline-first Android app connecting to AI endpoints.
