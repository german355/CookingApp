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
        categoryList.add(new CategoryItem(getString(R.string.category_breakfast_name), "завтрак", "meal_type", getString(R.string.category_breakfast_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image2.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_lunch_name), "обед", "meal_type", getString(R.string.category_lunch_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image1.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_dinner_name), "ужин", "meal_type", getString(R.string.category_dinner_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_snack_name), "закуска", "meal_type", getString(R.string.category_snack_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image3.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_main_course_name), "основное блюдо", "meal_type", getString(R.string.category_main_course_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image4.png"));

        // food_type
        categoryList.add(new CategoryItem(getString(R.string.category_pasta_name), "паста", "food_type", getString(R.string.category_pasta_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image14.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_side_dish_name), "гарнир", "food_type", getString(R.string.category_side_dish_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image5.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_salad_name), "салат", "food_type", getString(R.string.category_salad_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image6.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_soup_name), "суп", "food_type", getString(R.string.category_soup_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image7.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_sauce_name), "соус", "food_type", getString(R.string.category_sauce_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image8.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_dessert_name), "десерт", "food_type", getString(R.string.category_dessert_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image9.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_bakery_name), "выпечка", "food_type", getString(R.string.category_bakery_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image10.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_drink_name), "напиток", "food_type", getString(R.string.category_drink_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image11.png"));
        categoryList.add(new CategoryItem(getString(R.string.category_jam_name), "джем/варенье", "food_type", getString(R.string.category_jam_desc), "http://ec2-13-62-212-67.eu-north-1.compute.amazonaws.com/uploads/image12.png"));
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
