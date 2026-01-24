package com.example.cooking.ui.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.cooking.R;
import com.example.cooking.domain.entities.CategoryItem;
import java.util.ArrayList;
import java.util.List;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.cooking.ui.adapters.CategoryAdapter;

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
        categoryList.add(new CategoryItem(getString(R.string.category_breakfast_name), "завтрак", "meal_type", getString(R.string.category_breakfast_desc), "https://messages-prod.27c852f3500f38c1e7786e2c9ff9e48f.r2.cloudflarestorage.com/62c55f36-135e-4b66-b5bf-1f4395b09f6e/1769289654916-019bf1e1-582d-787d-b338-b027208150fd.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Credential=af634fe044bd071ab4c5d356fdace60f%2F20260124%2Fauto%2Fs3%2Faws4_request&X-Amz-Date=20260124T212055Z&X-Amz-Expires=3600&X-Amz-Signature=5851ec825c0a1bf451ca0bff9ce9cd1a8f728672e40d1b924359ba9dd81faba9&X-Amz-SignedHeaders=host&x-amz-checksum-mode=ENABLED&x-id=GetObject"));
        categoryList.add(new CategoryItem(getString(R.string.category_lunch_name), "обед", "meal_type", getString(R.string.category_lunch_desc), "https://messages-prod.27c852f3500f38c1e7786e2c9ff9e48f.r2.cloudflarestorage.com/62c55f36-135e-4b66-b5bf-1f4395b09f6e/1769289760420-019bf1e2-ee77-7a49-949e-b3cb870cc1db.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Credential=af634fe044bd071ab4c5d356fdace60f%2F20260124%2Fauto%2Fs3%2Faws4_request&X-Amz-Date=20260124T212240Z&X-Amz-Expires=3600&X-Amz-Signature=656baaf04caa8271ae8f39c8e481eec74912437f80b998cbb6d868e80ab1755a&X-Amz-SignedHeaders=host&x-amz-checksum-mode=ENABLED&x-id=GetObject"));
        categoryList.add(new CategoryItem(getString(R.string.category_dinner_name), "ужин", "meal_type", getString(R.string.category_dinner_desc), "https://i.ibb.co/wFrYNzr7/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_snack_name), "закуска", "meal_type", getString(R.string.category_snack_desc), "https://i.ibb.co/mCxHTNw5/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_main_course_name), "основное блюдо", "meal_type", getString(R.string.category_main_course_desc), "https://i.ibb.co/G4g1typg/image.png"));

        // food_type
        categoryList.add(new CategoryItem(getString(R.string.category_pasta_name), "паста", "food_type", getString(R.string.category_pasta_desc), "https://pplx-res.cloudinary.com/image/upload/v1747844845/gpt4o_images/rgeympr8dnlx3xkpbuoj.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_side_dish_name), "гарнир", "food_type", getString(R.string.category_side_dish_desc), "https://i.ibb.co/SkVXMcc/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_salad_name), "салат", "food_type", getString(R.string.category_salad_desc), "https://i.ibb.co/23Qvp6Vq/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_soup_name), "суп", "food_type", getString(R.string.category_soup_desc), "https://i.ibb.co/5gpN4Rjj/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_sauce_name), "соус", "food_type", getString(R.string.category_sauce_desc), "https://i.ibb.co/5WZD9mGf/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_dessert_name), "десерт", "food_type", getString(R.string.category_dessert_desc), "https://i.ibb.co/Kj607VSn/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_bakery_name), "выпечка", "food_type", getString(R.string.category_bakery_desc), "https://i.ibb.co/zTs29gWK/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_drink_name), "напиток", "food_type", getString(R.string.category_drink_desc), "https://i.ibb.co/4Zs3NB83/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_jam_name), "джем/варенье", "food_type", getString(R.string.category_jam_desc), "https://i.ibb.co/kFFvbMg/image.png"));
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
            Toast.makeText(getContext(), getString(R.string.error_navigation), Toast.LENGTH_SHORT).show();
        }
    }
}
