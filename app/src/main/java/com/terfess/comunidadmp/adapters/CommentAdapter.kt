package com.terfess.comunidadmp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole.getCurrentUserRole
import com.terfess.comunidadmp.model.Comment

class CommentAdapter(
    private val comments: List<Comment>,
    private val postId: String,
    private val onDeleteClick: (String, String) -> Unit = { _, _ -> }
) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName = itemView.findViewById<TextView>(R.id.comment_user_name)
        private val commentText = itemView.findViewById<TextView>(R.id.comment_text)
        private val commentDate = itemView.findViewById<TextView>(R.id.comment_date)
        private val userImage = itemView.findViewById<ImageView>(R.id.comment_user_image)
        private val btnDelete = itemView.findViewById<View>(R.id.btn_delete_comment)

        fun bind(comment: Comment) {
            userName.text = comment.userName
            commentText.text = comment.commentText
            val dateStr = comment.fechaComment.toString()
            commentDate.text = if (dateStr.length >= 10) dateStr.substring(0, 10) else dateStr

            val role = getCurrentUserRole()
            btnDelete.visibility = if (role == Roles.ADMIN) View.VISIBLE else View.GONE
            btnDelete.setOnClickListener {
                onDeleteClick(postId, comment.idComment)
            }

            // Load user photo
            db.collection("users")
                .whereEqualTo("userEmail", comment.userEmail)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val photoUser = querySnapshot.documents[0].getString("userPhoto")
                        if (!photoUser.isNullOrBlank()) {
                            Glide.with(itemView)
                                .load(photoUser)
                                .placeholder(R.drawable.predet_image)
                                .circleCrop()
                                .into(userImage)
                        }
                    }
                }
        }
    }
}