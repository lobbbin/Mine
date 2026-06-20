import os
import re

fixes = {
    # DashboardScreen.kt
    r"REGULATORY BLOCKADE: Clinic operations suspended due to unpaid fines \(>\$1\)": "REGULATORY BLOCKADE: Clinic operations suspended due to unpaid fines (>$10,000)",
    r"\+\$1 Cash": "+$100,000 Cash",
    r"-\$1 Cash": "-$15,000 Cash",
    r"Ask Chief Medical Officer for Advice \(-2 Prestige, \$1\)": "Ask Chief Medical Officer for Advice (-2 Prestige, $50)",
    r"cost \$1 from the clinic's expenses": "cost $800 from the clinic's expenses",
    r"Call \(\$1\)": "Call ($800)",
    r"Wire Bribe \(Cost: \$1\)": "Wire Bribe (Cost: $15,000)",
    r"Settle Juror Sub-rosa \(\$1\)": "Settle Juror Sub-rosa ($1,200)",
    r"Strike Law: \$\{policy.title\} \(Cost \$1\)": "Strike Law: ${policy.title} (Cost $500)",
    r"under \$1'\)": "under $800')",
    r"under \$1\)\.": "under $600).",
    r"treasury transfer of \$1 to": "treasury transfer of $1,500.00 to",
    r"Fine of \$1 for non-compliance, \$1 save": "Fine of $500 for non-compliance, $15 save",
    r"or \$1 consultant fee": "or $500 consultant fee",

    # ParliamentViewModel.kt
    r"Paid \$1 in practice consultant fees": "Paid $500 in practice consultant fees",

    # SettingsDataStore.kt
    r"balance of \$1": "balance of $50,000",
    r"Default \$1": "Default $850",
    r"Default \$1": "Default $150", 
    r"Default \$1": "Default $800",

    # ResultsBottomSheet.kt
    r"\(\$1/case\)": "($200/case)",

    # SandboxPresetActions.kt
    r"\(Cost \$1\)": "(Cost $7,500)", # wait, some are different. I will use file-specific regexes.
}

