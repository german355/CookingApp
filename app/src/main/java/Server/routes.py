import os
import sys
import logging
import time

# --- Добавляем корень проекта в sys.path ---
# Путь: /home/container
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if project_root not in sys.path:
    sys.path.insert(0, project_root)
# ------------------------------------------

from flask import Blueprint, request, jsonify
from services.search_service import search_service 

bp = Blueprint('search', __name__, url_prefix='/search')
logger = logging.getLogger(__name__)
@bp.route('/', methods=['GET'], strict_slashes=False)
async def search_route():
    """
    Эндпоинт для поиска рецептов по запросу
    """
    start_time = time.time()
    query = request.args.get('q')
    
    if not query:
        return jsonify({
            'status': 'error',
            'message': 'Параметр поиска "q" обязателен'
        }), 400
    
    logger.info(f"Поисковый запрос: {query}")
    
    try:
        # Получаем результаты поиска
        recipes_list, metadata_dict = await search_service.perform_smart_search(query=query)
        
        # Формируем ответ
        elapsed = time.time() - start_time
        total_results = len(recipes_list)
        
        logger.info(f"Поиск выполнен за {elapsed:.3f} сек, найдено результатов: {total_results}")
        
        return jsonify({
            'status': 'success',
            'data': {
                'results': recipes_list,
                'total_results': total_results
            }
        })
        
    except Exception as e:
        logger.exception("Ошибка при поиске: %s", e)
        return jsonify({
            'status': 'error',
            'message': f'Ошибка при выполнении поиска: {str(e)}'
        }), 500