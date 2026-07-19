package com.lastmilebanking.app.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lastmilebanking.app.R
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : ListAdapter<TransactionEntity, TransactionAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionEntity) {
            binding.tvReceiverName.text = transaction.receiverName.ifBlank { "Unknown" }
            binding.tvPaymentMode.text = transaction.paymentMode

            val isCredit = transaction.transactionType == "RECEIVE" || transaction.transactionType == "TOPUP"
            val amountText = if (isCredit) {
                "+ ${currencyFormat.format(transaction.amount)}"
            } else {
                "- ${currencyFormat.format(transaction.amount)}"
            }
            binding.tvAmount.text = amountText
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isCredit) R.color.color_success else R.color.color_error
                )
            )

            binding.tvDate.text = dateFormat.format(Date(transaction.createdAt))
            binding.tvStatus.text = transaction.status

            // Status chip color
            val statusColor = when (transaction.status) {
                "COMPLETED", "SYNCED" -> R.color.color_success
                "PENDING" -> R.color.color_warning
                "FAILED" -> R.color.color_error
                else -> R.color.color_warning
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, statusColor))

            // Payment mode icon
            val iconRes = when (transaction.paymentMode) {
                "QR" -> R.drawable.ic_qr_code
                "BLUETOOTH" -> R.drawable.ic_bluetooth
                "SMS" -> R.drawable.ic_sms
                else -> R.drawable.ic_payment
            }
            binding.ivPaymentMode.setImageResource(iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TransactionEntity>() {
            override fun areItemsTheSame(old: TransactionEntity, new: TransactionEntity) =
                old.transactionId == new.transactionId

            override fun areContentsTheSame(old: TransactionEntity, new: TransactionEntity) =
                old == new
        }
    }
}
