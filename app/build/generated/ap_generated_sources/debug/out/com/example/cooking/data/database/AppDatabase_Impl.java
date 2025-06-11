package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile RecipeDao _recipeDao;

  private volatile LikedRecipeDao _likedRecipeDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER NOT NULL, `title` TEXT, `ingredients` TEXT, `instructions` TEXT, `created_at` TEXT, `userId` TEXT, `mealType` TEXT, `foodType` TEXT, `photo_url` TEXT, `isLiked` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `liked_recipes` (`recipeId` INTEGER NOT NULL, PRIMARY KEY(`recipeId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '677125a5bc36642c5249fadf4daa665a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `recipes`");
        db.execSQL("DROP TABLE IF EXISTS `liked_recipes`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsRecipes = new HashMap<String, TableInfo.Column>(10);
        _columnsRecipes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("title", new TableInfo.Column("title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("ingredients", new TableInfo.Column("ingredients", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("instructions", new TableInfo.Column("instructions", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("userId", new TableInfo.Column("userId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("mealType", new TableInfo.Column("mealType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("foodType", new TableInfo.Column("foodType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("photo_url", new TableInfo.Column("photo_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecipes.put("isLiked", new TableInfo.Column("isLiked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRecipes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRecipes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRecipes = new TableInfo("recipes", _columnsRecipes, _foreignKeysRecipes, _indicesRecipes);
        final TableInfo _existingRecipes = TableInfo.read(db, "recipes");
        if (!_infoRecipes.equals(_existingRecipes)) {
          return new RoomOpenHelper.ValidationResult(false, "recipes(com.example.cooking.data.database.RecipeEntity).\n"
                  + " Expected:\n" + _infoRecipes + "\n"
                  + " Found:\n" + _existingRecipes);
        }
        final HashMap<String, TableInfo.Column> _columnsLikedRecipes = new HashMap<String, TableInfo.Column>(1);
        _columnsLikedRecipes.put("recipeId", new TableInfo.Column("recipeId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLikedRecipes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLikedRecipes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLikedRecipes = new TableInfo("liked_recipes", _columnsLikedRecipes, _foreignKeysLikedRecipes, _indicesLikedRecipes);
        final TableInfo _existingLikedRecipes = TableInfo.read(db, "liked_recipes");
        if (!_infoLikedRecipes.equals(_existingLikedRecipes)) {
          return new RoomOpenHelper.ValidationResult(false, "liked_recipes(com.example.cooking.data.database.LikedRecipeEntity).\n"
                  + " Expected:\n" + _infoLikedRecipes + "\n"
                  + " Found:\n" + _existingLikedRecipes);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "677125a5bc36642c5249fadf4daa665a", "25af9bf14cd8da59007dbc2cb2bd945d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "recipes","liked_recipes");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `recipes`");
      _db.execSQL("DELETE FROM `liked_recipes`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(RecipeDao.class, RecipeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LikedRecipeDao.class, LikedRecipeDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public RecipeDao recipeDao() {
    if (_recipeDao != null) {
      return _recipeDao;
    } else {
      synchronized(this) {
        if(_recipeDao == null) {
          _recipeDao = new RecipeDao_Impl(this);
        }
        return _recipeDao;
      }
    }
  }

  @Override
  public LikedRecipeDao likedRecipeDao() {
    if (_likedRecipeDao != null) {
      return _likedRecipeDao;
    } else {
      synchronized(this) {
        if(_likedRecipeDao == null) {
          _likedRecipeDao = new LikedRecipeDao_Impl(this);
        }
        return _likedRecipeDao;
      }
    }
  }
}
