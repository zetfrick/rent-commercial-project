// WebSocket Chat Client
let stompClient = null;
let isConnected = false;
let reconnectAttempts = 0;
let maxReconnectAttempts = 5;

function connectWebSocket() {
    if (!currentUserId) {
        console.error('User ID not available');
        return;
    }

    const socket = new SockJS('/ws-chat');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Отключаем отладку STOMP

    stompClient.connect({},
        function(frame) {
            console.log('WebSocket Connected: ' + frame);
            isConnected = true;
            reconnectAttempts = 0;

            // Подписываемся на личную очередь сообщений
            stompClient.subscribe('/user/' + currentUserId + '/queue/messages', function(response) {
                const message = JSON.parse(response.body);
                console.log('Received message via WebSocket:', message);

                // Добавляем новое сообщение в чат
                if (window.addNewMessageToChat) {
                    window.addNewMessageToChat(message);
                }

                // Обновляем список чатов в шапке
                if (window.updateChatsList) {
                    window.updateChatsList();
                }
            });

            // Подписываемся на индикатор набора текста
            stompClient.subscribe('/user/' + currentUserId + '/queue/typing', function(response) {
                const data = JSON.parse(response.body);
                if (window.showTypingIndicator) {
                    window.showTypingIndicator(data.senderLogin);
                }
            });

            // Подписываемся на уведомления о прочтении
            stompClient.subscribe('/user/' + currentUserId + '/queue/read', function(response) {
                const data = JSON.parse(response.body);
                if (window.markMessagesAsReadByOther) {
                    window.markMessagesAsReadByOther(data.senderId);
                }
            });

            console.log('WebSocket subscriptions established');
        },
        function(error) {
            console.error('WebSocket connection error:', error);
            isConnected = false;

            // Попытка переподключения
            if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++;
                setTimeout(connectWebSocket, 3000 * reconnectAttempts);
            }
        }
    );
}

function sendMessageViaWebSocket(text, premiseId) {
    if (!stompClient || !isConnected) {
        console.warn('WebSocket not connected, falling back to HTTP');
        return false;
    }

    const message = {
        senderId: currentUserId,
        receiverId: receiverId,
        senderLogin: currentUsername,
        text: text,
        sentAt: new Date().toISOString(),
        premiseId: premiseId || null,
        type: 'MESSAGE'
    };

    stompClient.send('/app/chat.send', {}, JSON.stringify(message));
    return true;
}

function sendTypingIndicator() {
    if (!stompClient || !isConnected) return;

    const typingData = {
        senderId: currentUserId,
        receiverId: receiverId,
        senderLogin: currentUsername,
        premiseId: premiseId || null
    };

    stompClient.send('/app/chat.typing', {}, JSON.stringify(typingData));
}

function markMessagesAsReadViaWebSocket(senderId, premiseId) {
    if (!stompClient || !isConnected) return;

    const readData = {
        senderId: senderId,
        receiverId: currentUserId,
        premiseId: premiseId || null
    };

    stompClient.send('/app/chat.read', {}, JSON.stringify(readData));
}

// Экспортируем функции для использования в других скриптах
window.connectWebSocket = connectWebSocket;
window.sendMessageViaWebSocket = sendMessageViaWebSocket;
window.sendTypingIndicator = sendTypingIndicator;
window.markMessagesAsReadViaWebSocket = markMessagesAsReadViaWebSocket;