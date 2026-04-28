package com.example.buksu_eeu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<UserModel> userList;

    public UserAdapter(Context context, List<UserModel> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_modern, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.fullName.setText(user.getUsername() != null ? user.getUsername() : "No Name");
        holder.email.setText(user.getEmail() != null ? user.getEmail() : "No Email");
        holder.phone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "No Phone Number");
        
        String initials = "U";
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            initials = user.getUsername().substring(0, 1).toUpperCase();
        }
        holder.initials.setText(initials);

        if (user.getProfilePhoto() != null && !user.getProfilePhoto().isEmpty()) {
            holder.profileImage.setVisibility(View.VISIBLE);
            holder.initials.setVisibility(View.GONE);
            Glide.with(context)
                    .load(user.getProfilePhoto())
                    .centerCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setVisibility(View.GONE);
            holder.initials.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView fullName, email, phone, initials;
        ImageView profileImage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            fullName = itemView.findViewById(R.id.user_fullname);
            email = itemView.findViewById(R.id.user_email);
            phone = itemView.findViewById(R.id.user_phone);
            initials = itemView.findViewById(R.id.user_initials);
            profileImage = itemView.findViewById(R.id.user_profile_image);
        }
    }
}
