import os
from flask import Flask, request, jsonify
import openai
import json

app = Flask(__name__)

# 1. OpenAI API 키를 환경 변수에서 로드합니다.
#    이 코드를 실행하기 전에 터미널/콘솔에서 'export OPENAI_API_KEY="YOUR_API_KEY"' (Linux/macOS)
#    또는 'set OPENAI_API_KEY=YOUR_API_KEY' (Windows)를 실행해야 합니다.
openai.api_key = os.getenv("OPENAI_API_KEY")

# API 키가 설정되지 않았다면 오류를 발생시켜 사용자에게 알립니다.
if not openai.api_key:
    raise ValueError("OPENAI_API_KEY 환경 변수가 설정되지 않았습니다. API 키를 환경 변수에 설정해주세요.")

# JSON 데이터를 코드 내에 직접 정의
predefined_data = [
    {
        "prompt": "이 저택은 뭐하는 곳이야?",
        "completion": "몇 년 전부터 방문하는 사람이 실종된다는 소문이 있는 곳입니다."
    },
    {
        "prompt": "도움이 필요해",
        "completion": "무슨 도움이 필요한가요?"
    },
    {
        "prompt": "컴퓨터 비밀번호",
        "completion": "저택의 전 주인과 관련된 단서를 찾아보는게 좋겠군요."
    },
    {
        "prompt": "컴퓨터 힌트",
        "completion": "저택의 전 주인과 관련된 단서를 찾아보는게 좋겠군요. 컴퓨터는 조심히 다루는게 좋을 겁니다."
    },
    {
        "prompt": "이 액자에 있는 사람은 누구야?",
        "completion": "저택의 전 주인일까요?"
    },
    {
        "prompt": "2층 열쇠",
        "completion": "액자를 살펴 보면 알 수 있을 겁니다."
    },
    {
        "prompt": "시계가 있어",
        "completion": "저택의 전 주인과 관련이 있는 걸까요?"
    },
    {
        "prompt": "저택 주인이 죽은 시간은?",
        "completion": "11시 45분입니다..."
    },
    {
        "prompt": "3층 열쇠",
        "completion": "이 저택에 원하는 물건을 적으면 가져다 주는 책이 있다고 합니다. 찾아서 사용해 보는게 어떨까요."
    },
    {
        "prompt": "쓸 수 있는 펜 같은 것이 있을까?",
        "completion": "2층을 잘 조사해보세요."
    },
    {
        "prompt": "일기장이 있어",
        "completion": "아마 저택 전 주인의 것일 겁니다."
    }
]

# JSON 데이터를 system 메시지로 포함
knowledge_message = (
    "아래는 사전에 정의된 질문(prompt)과 이에 대응하는 답변(completion)의 목록입니다.\n"
    "사용자가 질문하면, 이 목록에서 사용자 질문과 유사하거나 부분적으로라도 포함하는 prompt를 찾아 해당 completion을 그대로 답변해주세요.\n"
    "만약 정확히 일치하거나 부분적으로라도 포함하는 prompt를 찾지 못한다면, 적당히 둘러대는 답변을 하십시오.\n\n"
    + json.dumps(predefined_data, ensure_ascii=False, indent=2)
    + "\n\n당신은 정체불명의 제보자입니다. 단서를 제공하며 저택의 비밀을 풀게 도와주세요."
)

# 대화의 맥락을 저장하는 전역 변수
# system 메시지는 초기화 시 한 번만 추가됩니다.
conversation_context = [
    {"role": "system", "content": knowledge_message}
]

def generate_response(user_input, current_conversation_context):
    """
    OpenAI GPT 모델을 사용하여 챗봇 응답을 생성합니다.
    """
    # 사용자 메시지를 현재 대화 맥락에 추가
    current_conversation_context.append({"role": "user", "content": user_input})

    try:
        # GPT API 호출
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=current_conversation_context,
            temperature=0.2,  # 낮은 temperature로 모델이 사전 정의 답변을 더 정확히 따르도록 유도
            max_tokens=150
        )
        chatbot_reply = response['choices'][0]['message']['content']

    except openai.error.OpenAIError as e:
        # OpenAI API 관련 오류 처리
        print(f"OpenAI API 오류 발생: {e}")
        chatbot_reply = "API 호출 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
    except Exception as e:
        # 기타 예상치 못한 오류 처리
        print(f"예상치 못한 오류 발생: {e}")
        chatbot_reply = "알 수 없는 오류가 발생했습니다. 개발자에게 문의하세요."

    # GPT 응답을 현재 대화 맥락에 추가
    current_conversation_context.append({"role": "assistant", "content": chatbot_reply})

    return chatbot_reply, current_conversation_context

@app.route("/", methods=["GET"])
def home():
    """
    루트 경로에 대한 GET 요청을 처리합니다. API가 정상 작동 중임을 알립니다.
    """
    return "챗봇 API가 정상적으로 실행되고 있습니다. /chat 엔드포인트로 POST 요청을 보내주세요."

@app.route("/chat", methods=["POST"])
def chat():
    """
    /chat 엔드포인트에 대한 POST 요청을 처리하여 챗봇 응답을 반환합니다.
    """
    data = request.get_json()
    if not data or "user_input" not in data:
        return jsonify({"error": "user_input을 JSON 형식으로 제공해주세요."}), 400

    user_input = data["user_input"]

    # 전역 변수인 conversation_context를 사용합니다.
    global conversation_context
    response, conversation_context = generate_response(user_input, conversation_context)

    return jsonify({"response": response})

if __name__ == "__main__":
    # Flask 앱을 0.0.0.0 호스트의 5000번 포트에서 실행합니다.
    # debug=True는 개발 환경에서 유용하지만, 실제 서비스에서는 False로 설정해야 합니다.
    app.run(host='0.0.0.0', port=5000, debug=True)