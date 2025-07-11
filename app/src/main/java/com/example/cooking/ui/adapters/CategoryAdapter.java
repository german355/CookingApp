package com.example.cooking.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;
import com.example.cooking.domain.entities.CategoryItem;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * Адаптер для отображения списка категорий в RecyclerView.
 * Обрабатывает привязку данных категории к элементам списка и клики по ним.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategoryItem> categoryList;
    private OnCategoryClickListener listener;

    /**
     * Интерфейс для обработки событий клика по категории.
     */
    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem categoryItem);
    }

    public CategoryAdapter(List<CategoryItem> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem categoryItem = categoryList.get(position);
        holder.bind(categoryItem, listener);
    }

    @Override
    public int getItemCount() {
        return categoryList == null ? 0 : categoryList.size();
    }

    /**
     * ViewHolder для элемента списка категорий.
     * Содержит ссылки на View-компоненты карточки категории.
     */
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.category_image);

        }

        /**
         * Привязывает данные объекта CategoryItem к View-компонентам элемента списка.
         * @param categoryItem Объект с данными категории.
         * @param listener Слушатель для обработки клика.
         */
        public void bind(final CategoryItem categoryItem, final OnCategoryClickListener listener) {


            String imageUrl = categoryItem.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image) 
                        .error(R.drawable.error_image)       
                        .centerCrop()
                        .into(categoryImage);
            } else {
                categoryImage.setImageResource(R.drawable.placeholder_image);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(categoryItem);
                }
            });
        }
    }
} 