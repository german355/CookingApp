package com.example.cooking.ui.Catalog;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.cooking.R;
import com.example.cooking.model.CategoryItem;
import java.util.ArrayList;
import java.util.List;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import android.util.Log;

public class CatalogFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private List<CategoryItem> categoryList;

    public CatalogFragment() {
        // Required empty public constructor
    }

    public static CatalogFragment newInstance() {
        return new CatalogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_catalog, container, false);

        recyclerView = view.findViewById(R.id.categories_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // Например, 2 колонки

        loadCategoriesData();

        categoryAdapter = new CategoryAdapter(categoryList, this);
        recyclerView.setAdapter(categoryAdapter);

        return view;
    }

    private void loadCategoriesData() {
        categoryList = new ArrayList<>();
        // Данные из вашего запроса
        // meal_type
        categoryList.add(new CategoryItem("Завтрак", "завтрак", "meal_type", "Утренний прием пищи", "https://i.ibb.co/MkbgBf6z/image.png"));
        categoryList.add(new CategoryItem("Обед", "обед", "meal_type", "Дневной прием пищи", "https://i.ibb.co/8gR7qXH7/image.png"));
        categoryList.add(new CategoryItem("Ужин", "ужин", "meal_type", "Вечерний прием пищи", "https://i.ibb.co/wFrYNzr7/image.png"));
        categoryList.add(new CategoryItem("Закуска", "закуска", "meal_type", "Небольшое блюдо перед основной едой или к напиткам", "https://i.ibb.co/mCxHTNw5/image.png"));
        categoryList.add(new CategoryItem("Основное блюдо", "основное блюдо", "meal_type", "Главное, самое сытное блюдо трапезы", "https://i.ibb.co/G4g1typg/image.png"));

        // food_type
        categoryList.add(new CategoryItem("Паста", "паста", "food_type", "Итальянская паста", "https://pplx-res.cloudinary.com/image/upload/v1747844845/gpt4o_images/rgeympr8dnlx3xkpbuoj.png"));
        categoryList.add(new CategoryItem("Гарнир", "гарнир", "food_type", "Дополнение к основному блюду", "https://i.ibb.co/SkVXMcc/image.png"));
        categoryList.add(new CategoryItem("Салат", "салат", "food_type", "Блюдо из нарезанных ингредиентов с заправкой", "https://i.ibb.co/23Qvp6Vq/image.png"));
        categoryList.add(new CategoryItem("Суп", "суп", "food_type", "Жидкое первое блюдо", "https://i.ibb.co/5gpN4Rjj/image.png"));
        categoryList.add(new CategoryItem("Соус", "соус", "food_type", "Жидкая приправа к блюду", "https://i.ibb.co/5WZD9mGf/image.png"));
        categoryList.add(new CategoryItem("Десерт", "десерт", "food_type", "Сладкое блюдо в конце трапезы", "https://i.ibb.co/Kj607VSn/image.png"));
        categoryList.add(new CategoryItem("Выпечка", "выпечка", "food_type", "Общее понятие для изделий из теста", "https://i.ibb.co/zTs29gWK/image.png"));
        categoryList.add(new CategoryItem("Напиток", "напиток", "food_type", "Жидкость для питья", "https://i.ibb.co/4Zs3NB83/image.png"));
        categoryList.add(new CategoryItem("Джем/Варенье", "джем/варенье", "food_type", "Сладкая заготовка из фруктов/ягод", "https://i.ibb.co/kFFvbMg/image.png"));
    }

    @Override
    public void onCategoryClick(CategoryItem categoryItem) {
        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            CatalogFragmentDirections.ActionCatalogToFilteredRecipes action =
                    CatalogFragmentDirections.actionCatalogToFilteredRecipes(
                            categoryItem.getName(),
                            categoryItem.getFilterKey(),
                            categoryItem.getFilterType()
                    );
            navController.navigate(action);
        } else {
            Log.e("CatalogFragment", "NavHostFragment не найден, навигация невозможна.");
            Toast.makeText(getContext(), "Ошибка навигации", Toast.LENGTH_SHORT).show();
        }
    }
} 