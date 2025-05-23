import os
import sys # Добавлено
from flask import Flask
from flask_socketio import SocketIO
import pymysql # Добавлено, так как pymysql используется в маршрутах

# Импортируем конфигурацию базы данных
from db_config import get_db_connection

# --- Добавляем корень проекта в sys.path --- 
# Это нужно, чтобы импорты из папок верхнего уровня (services, db_config)
# работали корректно внутри блюпринтов при запуске через main.py
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
if project_root not in sys.path:
    sys.path.insert(0, project_root)
# ------------------------------------------

# Инициализация SocketIO без привязки к приложению пока
socketio = SocketIO(cors_allowed_origins="*")

def create_app(test_config=None):
    # Создание и конфигурирование экземпляра Flask
    app = Flask(__name__, instance_relative_config=True)

    # Загрузка базовой конфигурации (можно вынести в config.py)
    app.config.from_mapping(
        SECRET_KEY='dev', # TODO: Заменить на безопасный ключ и вынести в конфигурацию!
        DATABASE_HOST=os.environ.get('DB_HOST', 'localhost'), # Пример загрузки из env
        # ... другие базовые настройки ...
        UPLOAD_FOLDER = os.path.join(app.instance_path, 'uploads'), # Используем instance_path
        ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
    )

    if test_config is None:
        # Загрузка конфигурации экземпляра, если она существует (например, config.py)
        # app.config.from_pyfile('config.py', silent=True)
        pass # Пока не используем отдельный файл конфигурации
    else:
        # Загрузка тестовой конфигурации
        app.config.from_mapping(test_config)

    # Убедимся, что папка для загрузок существует
    try:
        os.makedirs(app.config['UPLOAD_FOLDER'])
        print(f"Создана папка для загрузок: {app.config['UPLOAD_FOLDER']}")
    except OSError:
        pass # Папка уже существует

    # Инициализация расширений Flask
    socketio.init_app(app)

    # Регистрация Blueprints (маршрутов)
    # Импорты делаем здесь, ПОСЛЕ добавления project_root в sys.path
    from app.auth import routes as auth_routes # Используем app. для явности
    from app.recipes import routes as recipe_routes
    from app.search import routes as search_routes
    from app.users import routes as user_routes
    from app.interactions import routes as interaction_routes
    from app.recommendations import routes as recommendation_routes
    from app.uploads import routes as upload_routes

    app.register_blueprint(auth_routes.bp)
    app.register_blueprint(recipe_routes.bp)
    app.register_blueprint(search_routes.bp)
    app.register_blueprint(user_routes.bp)
    app.register_blueprint(interaction_routes.bp)
    app.register_blueprint(recommendation_routes.bp)
    app.register_blueprint(upload_routes.bp) # Регистрируем новый Blueprint

    # Простой маршрут для проверки работы
    @app.route('/hello')
    def hello():
        return 'Hello, World from App Factory!'

    # Можно добавить обработчики ошибок приложения здесь (@app.errorhandler)

    return app 