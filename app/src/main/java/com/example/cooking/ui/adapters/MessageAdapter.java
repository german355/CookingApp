package com.example.cooking.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Message;
import com.example.cooking.domain.entities.Message.MessageType;
import com.example.cooking.ui.activities.RecipeDetailActivity;
import com.example.cooking.ui.adapters.Recipe.RecipeListAdapter;
import com.example.cooking.ui.utils.MarkdownUtils;

import android.app.Activity;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Message> messages;
    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;
    private static final int TYPE_LOADING = 2;
    private static final int TYPE_RECIPES = 3;
    private static final int TYPE_RECIPE_FLOW_LOADING = 4;
    private static final int TYPE_CREATED_RECIPE = 5;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else if (viewType == TYPE_RECIPE_FLOW_LOADING) {
            View view = inflater.inflate(R.layout.item_recipe_flow_loading, parent, false);
            return new RecipeFlowLoadingViewHolder(view);
        } else if (viewType == TYPE_CREATED_RECIPE) {
            View view = inflater.inflate(R.layout.item_chat_created_recipe, parent, false);
            return new CreatedRecipeViewHolder(view);
        } else {
            if (viewType == TYPE_RECIPES) {
                View view = inflater.inflate(R.layout.item_chat_recipes, parent, false);
                return new RecipesViewHolder(view);
            }
            View view = inflater.inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof LoadingViewHolder) {
        } else if (holder instanceof RecipeFlowLoadingViewHolder) {
            ((RecipeFlowLoadingViewHolder) holder).bind(message);
        } else if (holder instanceof CreatedRecipeViewHolder) {
            ((CreatedRecipeViewHolder) holder).bind(message.getAttachedRecipe(), message.getSecondaryText());
        } else {
            if (holder instanceof RecipesViewHolder) {
                ((RecipesViewHolder) holder).bind(message.getAttachedRecipes());
            } else {
                ((MessageViewHolder) holder).bind(message);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.getType() == MessageType.LOADING) return TYPE_LOADING;
        if (msg.getType() == MessageType.USER) return TYPE_USER;
        if (msg.getType() == MessageType.AI) return TYPE_AI;
        if (msg.getType() == MessageType.RECIPES) return TYPE_RECIPES;
        if (msg.getType() == MessageType.RECIPE_FLOW_LOADING) return TYPE_RECIPE_FLOW_LOADING;
        if (msg.getType() == MessageType.CREATED_RECIPE) return TYPE_CREATED_RECIPE;
        return TYPE_AI;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewMessage);
        }

        public void bind(Message message) {
            textView.setText(MarkdownUtils.toSpannable(message.getText()));
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textView.getLayoutParams();
            if (message.isUser()) {
                params.gravity = Gravity.END;
                textView.setBackgroundResource(R.drawable.bg_message_bubble_user);
            } else {
                params.gravity = Gravity.START;
                textView.setBackgroundResource(R.drawable.bg_message_bubble);
            }
            textView.setLayoutParams(params);
        }
    }

    static class RecipesViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        RecipeListAdapter adapter;

        public RecipesViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view_recipes);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), RecyclerView.HORIZONTAL, false));
            adapter = new RecipeListAdapter((recipe, isLiked) -> {}, true);
            recyclerView.setAdapter(adapter);
        }

        public void bind(List<Recipe> recipes) {
            adapter.submitList(recipes);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class RecipeFlowLoadingViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView subtitleView;

        public RecipeFlowLoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.textRecipeFlowTitle);
            subtitleView = itemView.findViewById(R.id.textRecipeFlowSubtitle);
        }

        public void bind(Message message) {
            titleView.setText(message.getText());
            subtitleView.setText(message.getSecondaryText());
        }
    }

    static class CreatedRecipeViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView subtitleView;

        public CreatedRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.created_recipe_card);
            imageView = itemView.findViewById(R.id.created_recipe_image);
            titleView = itemView.findViewById(R.id.created_recipe_title);
            subtitleView = itemView.findViewById(R.id.created_recipe_subtitle);
        }

        public void bind(Recipe recipe, String subtitle) {
            if (recipe == null) {
                return;
            }
            titleView.setText(recipe.getTitle());
            subtitleView.setText(subtitle);
            if (recipe.getPhoto_url() != null && !recipe.getPhoto_url().isEmpty()) {
                Glide.with(imageView.getContext())
                        .load(recipe.getPhoto_url())
                        .placeholder(R.drawable.placeholder_food)
                        .error(R.drawable.placeholder_food)
                        .centerCrop()
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.placeholder_food);
            }

            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_SELECTED_RECIPE, recipe);
                Context context = v.getContext();
                Activity activity = null;
                Context baseContext = context;
                while (baseContext instanceof ContextWrapper) {
                    if (baseContext instanceof Activity) {
                        activity = (Activity) baseContext;
                        break;
                    }
                    baseContext = ((ContextWrapper) baseContext).getBaseContext();
                }
                if (activity != null) {
                    activity.startActivityForResult(intent, 200);
                } else {
                    context.startActivity(intent);
                }
            });
        }
    }
}
