package com.skillhando.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PremiumPage() {
    val billing by Billing.state.collectAsStateWithLifecycle()
    Heading(tr("skillhando_premium"), tr("more_room_to_practise_and_save_evidence"))
    Panel {
        Subheading(if (billing.active) tr("premium_is_active") else tr("one_time_purchase"))
        Text(tr("keep_more_than_two_active_pathways_add_more_portfolio_attachments_and_export_pdf"))
        Text(tr("your_current_practice_records_remain_available_regardless_of_purchase_status"))
        if (Billing.configured) {
            billing.price?.let { price ->
                if (!billing.active) Button(onClick = Billing::buy, enabled = !billing.working) { Text(trf("buy_premium", price)) }
            }
            OutlinedButton(onClick = Billing::restore, enabled = !billing.working) { Text(tr("restore_purchases")) }
            if (billing.message.isNotBlank()) Text(billing.message)
        } else Text(tr("purchases_are_unavailable_in_this_build"))
    }
}
