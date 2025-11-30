// Initialize Icons
lucide.createIcons();

// Mock Data
let currentUser = { id: null, name: 'Me', avatar: 'ME' };

// Fetch current user info from backend FIRST, then connect WebSocket
$.ajax({
    type: 'GET',
    url: '/api/token/info',
    success: function (response) {
        if (response.authenticated) {

       

            currentUser.id = String(response.userId);
            currentUser.name = response.username;

            // Update sidebar avatar
            const sidebarAvatar = document.getElementById('current-user-avatar');
            if (response.picture) {
                currentUser.avatar = `<img src="${response.picture}" class="w-full h-full object-cover rounded-full" />`;
                sidebarAvatar.innerHTML = currentUser.avatar;
            } else {
                currentUser.avatar = response.username.substring(0, 2).toUpperCase();
                sidebarAvatar.textContent = currentUser.avatar;
            }

       

            // NOW connect to WebSocket after we have the user ID
            connectWebSocket();
            fetchUserChats();

            // Re-render messages to ensure current user messages are on the right
            if (activeChatId) {
                renderMessages(activeChatId);
            }
        } else {
            console.warn("User not authenticated");
            // Handle unauthenticated state (e.g., redirect to login)
        }
    },
    error: function (xhr) {
        console.error("Failed to fetch user info", xhr);
    }
});

let users = [];
let channels = [];

// Mock Messages Store
const messages = {};

// State
let activeChatId = null;
let activeChatType = null;
let isLoadingOld = false;

// DOM Elements
const dmListEl = document.getElementById('dm-list');
const channelListEl = document.getElementById('channel-list');
const chatInfoEl = document.getElementById('chat-info');
const messagesContainer = document.getElementById('messages-container');
const addMemberBtn = document.getElementById('add-member-btn');
if (addMemberBtn) {
    addMemberBtn.addEventListener('click', toggleAddMemberModal);
}
const scrollLoader = document.getElementById('scroll-loader');
const modalUserList = document.getElementById('modal-user-list');

// WebSocket Client - will be activated after user info is loaded
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
});

// Function to connect WebSocket (called after user info is loaded)
function connectWebSocket() {


    stompClient.onConnect = (frame) => {

        stompClient.subscribe('/topic/chat', (greeting) => {
            const body = JSON.parse(greeting.body);
            

            // Update messages for 'general' channel
            messages['general'] = body.map((msg, index) => {
                // Get initials from name if avatar is missing
                const name = msg.senderName || 'User';
                const initials = name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

                // Backend sends 'sender' field as the user ID
                const senderId = msg.sender ? String(msg.sender) : 'unknown';

                return {
                    id: index,
                    sender: {
                        id: senderId,
                        name: name,
                        avatar: msg.senderAvatar ? `<img src="${msg.senderAvatar}" class="w-full h-full object-cover rounded-full" />` : initials,
                        color: 'bg-gray-100 text-gray-700' // Default color
                    },
                    text: msg.content,
                    time: msg.timestamp || ''
                };
            });

            if (activeChatId === 'general') {
                renderMessages('general');
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            }
        });

        // Subscribe to private messages
        if (currentUser && currentUser.id) {
            stompClient.subscribe('/queue/chat-user-' + currentUser.id, (greeting) => {
                const body = JSON.parse(greeting.body);


                // The backend sends a LIST of messages (latest 15)
                // We should process them. Since this is a "push" of the latest state,
                // we might want to merge them or just append the new ones.
                // However, the current logic in ChatService sends the whole list.

                // Let's assume the last one is the new one for notification purposes,
                // but we should update the chat view if it's active.

                if (Array.isArray(body)) {
                    body.forEach(msg => {
                        handleIncomingMessage(msg);
                    });
                } else {
                    handleIncomingMessage(body);
                }
            });

            // Subscribe to user updates (channel added/removed)
            stompClient.subscribe('/queue/updates-user-' + currentUser.id, (notification) => {
                const body = JSON.parse(notification.body);
                
                if (body.type === 'CHANNEL_ADDED') {
                    const newChannel = {
                        id: String(body.channel.id),
                        name: body.channel.name,
                        type: 'group',
                        members: 0 // Or fetch details
                    };
                    // Check if exists
                    if (!channels.some(c => c.id === newChannel.id)) {
                        channels.push(newChannel);
                        renderSidebar();
                    }
                } else if (body.type === 'CHANNEL_REMOVED') {
                    const channelId = String(body.channelId);
                    channels = channels.filter(c => c.id !== channelId);
                    renderSidebar();

                    // If active chat was this channel, clear it
                    if (activeChatId === channelId) {
                        activeChatId = null;
                        activeChatType = null;
                        messagesContainer.innerHTML = '<div class="flex justify-center items-center h-full text-gray-500">Select a chat to start messaging</div>';
                        chatInfoEl.innerHTML = '';
                        addMemberBtn.style.display = 'none';
                        document.getElementById('group-actions-btn')?.remove();
                    }
                }
            });
        }
    };

    function handleIncomingMessage(msg) {
       
        const senderId = msg.sender ? String(msg.sender).toLowerCase() : 'unknown';
        const currentUserId = String(currentUser.id).toLowerCase();

        // If message is from me, ignore (handled optimistically)
        if (senderId === currentUserId) {
            
            return;
        }

        let chatId;
        // Check if this is a group channel message
        // We look up the channelId in our local 'channels' list
        const isGroupChannel = channels.some(c => c.id === String(msg.channelId));

        if (isGroupChannel) {
            chatId = String(msg.channelId);
        } else {
            // It's a DM, so the chat ID is the sender's ID
            chatId = senderId;
        }

        // Ensure message list exists
        if (!messages[chatId]) {
            messages[chatId] = [];
        }

        // Check if message already exists (deduplication)
        const exists = messages[chatId].some(m => m.id === msg.messageId);
        if (exists) {
            return;
        }

        // Format message
        const formattedMsg = convertToFrontendMessage(msg);

        // Add to list
        messages[chatId].push(formattedMsg);

        // If this is the active chat, render it
        // Normalize activeChatId for comparison
        const normalizedActiveChatId = activeChatId ? String(activeChatId).toLowerCase() : null;

        if (normalizedActiveChatId === chatId) {
            renderMessages(activeChatId); // Keep original case for render function if needed
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        } else {
            // Optional: Show unread badge
        }
    }

    stompClient.onWebSocketError = (error) => {
        console.error('❌ Error with websocket', error);
    };

    stompClient.onStompError = (frame) => {
        console.error('❌ Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
    };

    stompClient.activate();
}

function fetchUserChats() {
    // Fetch Channels
    $.get('/api/chat/user/channels', function (data) {
        if (Array.isArray(data)) {
            channels = data.map(c => ({
                id: String(c.id),
                name: c.name,
                type: 'group',
                members: 0 // We don't have member count yet
            }));
            renderSidebar();
        } else {
            console.error("Expected array for channels but got:", data);
        }
    }).fail(function (xhr) {
        console.error("Failed to fetch channels", xhr);
    });

    // Fetch Conversations (DMs)
    $.get('/api/chat/user/conversations', function (data) {
        if (Array.isArray(data)) {
            users = data.map(u => ({
                id: String(u.id),
                name: u.name || u.username,
                avatar: u.picture ? `<img src="${u.picture}" class="w-full h-full object-cover rounded-full" />` : (u.name || u.username).substring(0, 2).toUpperCase(),
                color: 'bg-gray-100 text-gray-700', // Default color
                status: 'offline', // We don't have status yet
                type: 'dm'
            }));
            renderSidebar();
        } else {
            console.error("Expected array for conversations but got:", data);
        }
    }).fail(function (xhr) {
        console.error("Failed to fetch conversations", xhr);
    });
}


// Render Sidebar
function renderSidebar() {
    // DMs
    dmListEl.innerHTML = users.map(u => {
        const isActive = activeChatId === u.id;
        return `
        <div onclick="loadChat('${u.id}', 'dm')" 
             class="flex items-center gap-3 px-3 py-3 rounded-lg cursor-pointer transition-all ${isActive ? 'bg-blue-50' : 'hover:bg-gray-100'}">
            <div class="relative flex-shrink-0">
                <div class="w-14 h-14 rounded-full ${u.color} flex items-center justify-center text-sm font-bold shadow-sm overflow-hidden">
                    ${u.avatar}
                </div>
                <span class="absolute bottom-0 right-0 w-4 h-4 border-2 border-white rounded-full ${u.status === 'online' ? 'bg-green-500' : u.status === 'away' ? 'bg-yellow-400' : 'bg-gray-300'}"></span>
            </div>
            <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                    <span class="text-sm font-semibold text-gray-900 truncate">${u.name}</span>
                    <span class="text-xs text-gray-500">2m</span>
                </div>
                <p class="text-xs text-gray-500 truncate">Active ${u.status}</p>
            </div>
        </div>
    `;
    }).join('');

    // Channels
    channelListEl.innerHTML = channels.map(c => {
        const isActive = activeChatId === c.id;
        return `
        <div onclick="loadChat('${c.id}', 'group')" 
             class="flex items-center gap-3 px-3 py-3 rounded-lg cursor-pointer transition-all ${isActive ? 'bg-blue-50' : 'hover:bg-gray-100'}">
            <div class="w-14 h-14 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-sm font-bold shadow-sm">
                <i data-lucide="users" class="w-6 h-6"></i>
            </div>
            <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                    <span class="text-sm font-semibold text-gray-900 truncate">${c.name}</span>
                    <span class="text-xs text-gray-500">5m</span>
                </div>
                <p class="text-xs text-gray-500 truncate">${c.members} members</p>
            </div>
        </div>
    `;
    }).join('');

    lucide.createIcons();
}

// Generate initial mock messages for a chat
function generateMessages(id) {
    if (messages[id]) return;
    messages[id] = [];
    // Don't generate mock messages for 'general' as it comes from backend
    if (id === 'general') return;

    for (let i = 0; i < 15; i++) {
        const isMe = Math.random() > 0.5;
        messages[id].push({
            id: Date.now() + i,
            sender: isMe ? currentUser : users[Math.floor(Math.random() * users.length)],
            text: isMe ? "Just checking in on the progress." : "Updated the designs, take a look when you can.",
            time: '10:3' + i + ' AM'
        });
    }
}

// Helper to convert backend message to frontend format
function convertToFrontendMessage(msg) {
    const name = msg.senderName || 'User';
    const initials = name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

    return {
        id: msg.messageId,
        sequenceNumber: msg.sequenceNumber,
        channelId: msg.channelId,
        sender: {
            id: msg.sender,
            name: name,
            avatar: msg.senderAvatar ? `<img src="${msg.senderAvatar}" class="w-full h-full object-cover rounded-full" />` : initials,
            color: 'bg-gray-100 text-gray-700'
        },
        text: msg.content,
        time: msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''
    };
}

// Load Chat View
function loadChat(id, type) {
    activeChatId = id;
    activeChatType = type;
    renderSidebar();

    // Show loading state
    messagesContainer.innerHTML = '<div class="flex justify-center items-center h-full"><i data-lucide="loader-2" class="w-8 h-8 text-blue-600 spinner"></i></div>';
    lucide.createIcons();

    const target = type === 'dm' ? users.find(u => u.id === id) : channels.find(c => c.id === id);

    // Update Header
    if (type === 'dm') {
        chatInfoEl.innerHTML = `
            <div class="relative">
                <div class="w-10 h-10 rounded-full ${target.color} flex items-center justify-center text-sm font-bold shadow-sm overflow-hidden">
                    ${target.avatar}
                </div>
                <span class="absolute bottom-0 right-0 w-3 h-3 border-2 border-white rounded-full ${target.status === 'online' ? 'bg-green-500' : 'bg-gray-300'}"></span>
            </div>
            <div>
                <h2 class="text-base font-semibold text-gray-900">${target.name}</h2>
                <p class="text-xs text-gray-500 capitalize">${target.status}</p>
            </div>
        `;
        addMemberBtn.style.display = 'none';
        document.getElementById('group-actions-btn')?.remove();
    } else {
        chatInfoEl.innerHTML = `
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white shadow-sm">
                <i data-lucide="users" class="w-5 h-5"></i>
            </div>
            <div>
                <h2 class="text-base font-semibold text-gray-900">${target.name}</h2>
                <p class="text-xs text-gray-500">${target.members}/500 members</p>
            </div>
        `;
        addMemberBtn.style.display = 'block';

        // Add Group Actions Button (Leave/Delete)
        // Check if button already exists to avoid duplicates
        if (!document.getElementById('group-actions-btn')) {
            const actionsBtn = document.createElement('button');
            actionsBtn.id = 'group-actions-btn';
            actionsBtn.className = "p-2 hover:bg-gray-100 rounded-full text-gray-500 transition-colors relative";
            actionsBtn.innerHTML = `<i data-lucide="more-vertical" class="w-5 h-5"></i>
                <div id="group-actions-menu" class="hidden absolute right-0 top-full mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-50">
                    <button onclick="leaveGroup('${id}')" class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50">Leave Group</button>
                    <button onclick="deleteGroup('${id}')" class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50">Delete Group</button>
                </div>`;

            // Insert after add member button
            addMemberBtn.parentNode.insertBefore(actionsBtn, addMemberBtn.nextSibling);

            // Toggle menu
            actionsBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                const menu = document.getElementById('group-actions-menu');
                menu.classList.toggle('hidden');
            });

            // Close menu when clicking outside
            document.addEventListener('click', (e) => {
                if (!actionsBtn.contains(e.target)) {
                    document.getElementById('group-actions-menu')?.classList.add('hidden');
                }
            });

            lucide.createIcons();
        }

        // Update modal
        document.getElementById('modal-channel-name').innerText = target.name;
        document.getElementById('modal-count').innerText = target.members;
    }

    // Fetch messages from server
    let url = '';
    if (type === 'dm') {
        if (!currentUser.id) {
            console.error("Current user ID is missing. Cannot load DM.");
            return;
        }
        url = `/api/chat/conversation?userId1=${currentUser.id}&userId2=${id}`;
    } else {
        // Assuming id is numeric for groups. If 'general' is special, handle it.
        if (id === 'general') {
            // Fallback or specific ID for general? 
            // For now, let's try to use the ID if it's numeric, otherwise skip or use a hardcoded ID if known.
            // If 'general' is string, this might fail on backend if it expects Long.
            // But let's assume the user has a way to handle 'general' or it's just a placeholder.
            // If the user clicks a searched channel, it has a numeric ID.
            // Let's just try to call the endpoint. If it fails, we handle error.
            url = `/api/chat/channel/1/initial`; // Assuming 'general' is ID 1 for now, or we need to look it up.
        } else {
            url = `/api/chat/channel/${id}/initial`;
        }
    }

    $.get(url, function (data) {
        if (Array.isArray(data)) {
            messages[id] = data.map(msg => convertToFrontendMessage(msg));
            renderMessages(id);
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        } else {
            // Check if it's a login page (HTML) indicating session expiration
            if (typeof data === 'string' && (data.includes('<!DOCTYPE html>') || data.includes('<html'))) {
                console.warn("Received HTML instead of JSON. Session likely expired. Redirecting to login.");
                window.location.href = '/login';
                return;
            }

            console.error("Expected array of messages but got:", data);
            messages[id] = [];
            renderMessages(id);
        }
    }).fail(function (xhr) {
        if (xhr.status === 401 || xhr.status === 403) {
            window.location.href = '/login';
            return;
        }
        console.error("Failed to load messages");
        messages[id] = [];
        renderMessages(id);
    });
}

function renderMessages(id) {
    const msgs = messages[id] || [];

    messagesContainer.innerHTML = msgs.map((m, index) => {
        const myId = currentUser.id ? String(currentUser.id).toLowerCase().trim() : '';
        const msgSenderId = m.sender.id ? String(m.sender.id).toLowerCase().trim() : '';
        const isMe = myId && msgSenderId && myId === msgSenderId;

        if (index < 3) { // Log first 3 messages for debugging
        }

        // Check sequence status
        const prevSenderId = index > 0 ? (msgs[index - 1].sender.id ? String(msgs[index - 1].sender.id).toLowerCase().trim() : '') : null;
        const nextSenderId = index < msgs.length - 1 ? (msgs[index + 1].sender.id ? String(msgs[index + 1].sender.id).toLowerCase().trim() : '') : null;

        const isFirstInSequence = index === 0 || prevSenderId !== msgSenderId;
        const isLastInSequence = index === msgs.length - 1 || nextSenderId !== msgSenderId;

        // Messenger-style message rendering
        return `
            <div class="flex gap-2 ${isMe ? 'flex-row-reverse' : ''} items-end message-bubble ${isFirstInSequence ? 'mt-4' : 'mt-0.5'}">
                <!-- Avatar Column (Only for others, only on last message in sequence) -->
                <div class="flex-shrink-0 w-8">
                    ${!isMe && isLastInSequence ? `
                        <div class="w-8 h-8 rounded-full ${m.sender.color || 'bg-gray-200 text-gray-600'} flex items-center justify-center text-xs font-semibold shadow-sm overflow-hidden">
                            ${m.sender.avatar}
                        </div>
                    ` : ''}
                </div>
                
                <!-- Message Column -->
                <div class="flex flex-col ${isMe ? 'items-end' : 'items-start'} max-w-[60%]">
                    <!-- Sender Name (Only for others, only first in sequence, and in group chats) -->
                    ${!isMe && isFirstInSequence && activeChatId && channels.find(c => c.id === activeChatId) ?
                `<span class="text-xs text-gray-500 mb-1 px-3 font-medium">${m.sender.name}</span>`
                : ''}
                    
                    <!-- Message Bubble -->
                    <div class="group relative">
                        <div class="px-4 py-2.5 text-[15px] leading-relaxed shadow-sm transition-all
                            ${isMe
                ? 'bg-blue-600 text-white rounded-3xl rounded-br-md'
                : 'bg-gray-200 text-gray-900 rounded-3xl rounded-bl-md'}">
                            ${m.text}
                        </div>
                        
                        <!-- Timestamp on hover (shown on last in sequence) -->
                        ${isLastInSequence ? `
                            <div class="absolute ${isMe ? 'right-0' : 'left-0'} -bottom-5 opacity-0 group-hover:opacity-100 transition-opacity">
                                <span class="text-xs text-gray-400">${m.time || 'Just now'}</span>
                            </div>
                        ` : ''}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // Add spacer at bottom
    const spacer = document.createElement('div');
    spacer.className = "h-4";
    messagesContainer.appendChild(spacer);
}        // Infinite Scroll
messagesContainer.addEventListener('scroll', () => {
    if (messagesContainer.scrollTop === 0 && !isLoadingOld && activeChatId) {
        const currentMessages = messages[activeChatId];
        if (!currentMessages || currentMessages.length === 0) return;

        const oldestSequence = currentMessages[0].sequenceNumber;
        const channelId = currentMessages[0].channelId;

        if (!oldestSequence || !channelId) return;

        isLoadingOld = true;
        scrollLoader.style.opacity = '1';

        const url = `/api/chat/channel/${channelId}/history?beforeSequence=${oldestSequence}`;

        $.get(url, function (data) {
            if (data && data.length > 0) {
                const oldMessages = data.map(msg => convertToFrontendMessage(msg));
                messages[activeChatId] = [...oldMessages, ...messages[activeChatId]];

                const previousHeight = messagesContainer.scrollHeight;
                renderMessages(activeChatId);
                messagesContainer.scrollTop = messagesContainer.scrollHeight - previousHeight;
            }
            isLoadingOld = false;
            scrollLoader.style.opacity = '0';
        }).fail(function () {
            isLoadingOld = false;
            scrollLoader.style.opacity = '0';
        });
    }
});

// Send Message
function sendMessage() {
    const input = document.getElementById('message-input');
    const text = input.value.trim();
    if (!text || !activeChatId) return;

    // Prepare payload
    const payload = {
        sender: {
            userId: currentUser.id,
            content: text
        },
        receiver: {
            userId: null,
            channelId: null
        }
    };

    if (activeChatType === 'dm') {
        payload.receiver.userId = activeChatId;
    } else {
        // Group chat
        if (activeChatId === 'general') {
            payload.receiver.channelId = 1; // Assuming general is channel 1
        } else {
            payload.receiver.channelId = activeChatId;
        }
    }

    // Optimistic UI update
    const tempId = Date.now();
    const optimisticMsg = {
        id: tempId,
        sender: currentUser,
        text: text,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        // We don't have sequenceNumber or channelId yet, but that's fine for display
    };

    if (!messages[activeChatId]) messages[activeChatId] = [];
    messages[activeChatId].push(optimisticMsg);
    renderMessages(activeChatId);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;

    input.value = '';

    // Send request
    let url = '/api/chat/send';

    $.ajax({
        type: 'POST',
        url: url,
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (response) {
        },
        error: function (xhr, status, error) {
            console.error("Error sending message:", error);
            // Optionally remove the optimistic message
        }
    });
}

// Enter to send
document.getElementById('message-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

// Modal Logic
function toggleAddMemberModal() {
    const modal = document.getElementById('add-member-modal');
    if (modal.classList.contains('hidden')) {
        // Reset search input
        const searchInput = document.getElementById('add-member-search');
        if (searchInput) searchInput.value = '';

        // Clear list initially or show recent contacts
        renderAddMemberUserList(users); // Show current DM contacts by default

        lucide.createIcons();
        modal.classList.remove('hidden');
    } else {
        modal.classList.add('hidden');
    }
}

function renderAddMemberUserList(userList) {
    if (userList.length === 0) {
        modalUserList.innerHTML = '<div class="p-4 text-center text-gray-500">No users found</div>';
        return;
    }

    modalUserList.innerHTML = userList.map(u => `
        <label class="flex items-center justify-between p-3 hover:bg-blue-50 rounded-xl cursor-pointer group transition-all">
            <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-full ${u.color || 'bg-gray-100'} flex items-center justify-center text-xs font-bold shadow-sm overflow-hidden">
                    ${u.avatar}
                </div>
                <div>
                    <p class="text-sm font-semibold text-gray-900">${u.name}</p>
                    <p class="text-xs text-gray-500">@${(u.name || 'user').toLowerCase().replace(' ', '')}</p>
                </div>
            </div>
            <div class="relative flex items-center">
                <input type="checkbox" value="${u.id}" class="peer appearance-none h-5 w-5 border-2 border-gray-300 rounded transition-all checked:bg-blue-600 checked:border-blue-600 cursor-pointer">
                <i data-lucide="check" class="absolute w-4 h-4 text-white opacity-0 peer-checked:opacity-100 pointer-events-none left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2"></i>
            </div>
        </label>
    `).join('');
    lucide.createIcons();
}

// Add Member Search Listener
document.addEventListener('DOMContentLoaded', () => {
    // Inject search input into modal if not exists
    const modalContent = document.querySelector('#add-member-modal .bg-white');
    if (modalContent) {
        const header = modalContent.querySelector('h3');
        if (header && !document.getElementById('add-member-search')) {
            const searchContainer = document.createElement('div');
            searchContainer.className = "mb-4 relative";
            searchContainer.innerHTML = `
                <input type="text" id="add-member-search" placeholder="Search users to add..." 
                       class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all">
                <i data-lucide="search" class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"></i>
            `;
            header.parentNode.insertBefore(searchContainer, header.nextSibling);

            // Add event listener
            let addMemberSearchTimeout;
            document.getElementById('add-member-search').addEventListener('input', (e) => {
                const query = e.target.value.trim();
                clearTimeout(addMemberSearchTimeout);

                if (query.length < 1) {
                    renderAddMemberUserList(users); // Revert to DM list
                    return;
                }

                addMemberSearchTimeout = setTimeout(() => {
                    fetch(`/api/v1/users/search?query=${encodeURIComponent(query)}`)
                        .then(res => res.json())
                        .then(foundUsers => {
                            // Map to frontend format
                            const mappedUsers = foundUsers.map(u => ({
                                id: String(u.id),
                                name: u.name || u.username,
                                avatar: u.picture ? `<img src="${u.picture}" class="w-full h-full object-cover rounded-full" />` : (u.name || u.username).substring(0, 2).toUpperCase(),
                                color: 'bg-gray-100 text-gray-700'
                            }));
                            renderAddMemberUserList(mappedUsers);
                        })
                        .catch(err => console.error("Search failed", err));
                }, 300);
            });
        }
    }
});

// Create Channel Modal Logic
function toggleCreateChannelModal() {
    const modal = document.getElementById('create-channel-modal');
    if (modal.classList.contains('hidden')) {
        // Populate user list for selection
        const userListContainer = document.getElementById('create-channel-user-list');
        userListContainer.innerHTML = users.map(u => `
            <label class="flex items-center justify-between p-2 hover:bg-blue-50 rounded-lg cursor-pointer group transition-all">
                <div class="flex items-center gap-2">
                    <div class="w-8 h-8 rounded-full ${u.color} flex items-center justify-center text-xs font-bold shadow-sm overflow-hidden">${u.avatar}</div>
                    <span class="text-sm font-medium text-gray-900">${u.name}</span>
                </div>
                <input type="checkbox" value="${u.id}" class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500">
            </label>
        `).join('');

        document.getElementById('new-channel-name').value = '';
        modal.classList.remove('hidden');
    } else {
        modal.classList.add('hidden');
    }
}

// Add selected members function
function addSelectedMembers() {
    const checkboxes = document.querySelectorAll('#modal-user-list input[type="checkbox"]:checked');
    const selectedUserIds = Array.from(checkboxes).map(cb => cb.value);
    const count = selectedUserIds.length;

    if (count > 0 && activeChatId) {
        const channel = channels.find(c => c.id === activeChatId);
        if (channel) {
            // Call backend API to add members
            $.ajax({
                type: 'POST',
                url: `/api/chat/channel/${activeChatId}/add-members`,
                contentType: 'application/json',
                data: JSON.stringify(selectedUserIds),
                success: function (response) {
                
                    channel.members += count;
                    // Update the header
                    loadChat(activeChatId, 'group');
                    toggleAddMemberModal();
                },
                error: function (xhr) {
                    console.error("Failed to add members", xhr);
                    alert("Failed to add members");
                }
            });
        }
    }
}

// Create Channel Function
function createChannel() {
    const name = document.getElementById('new-channel-name').value.trim();
    const checkboxes = document.querySelectorAll('#create-channel-user-list input[type="checkbox"]:checked');
    const selectedMemberIds = Array.from(checkboxes).map(cb => cb.value);

    if (!name) {
        alert("Please enter a channel name");
        return;
    }

    const payload = {
        name: name,
        creatorId: currentUser.id,
        memberIds: selectedMemberIds
    };

    $.ajax({
        type: 'POST',
        url: '/api/chat/channel/create',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (response) {

            const newChannelId = String(response.id);

            // Check if already added (e.g. by WebSocket)
            if (!channels.some(c => c.id === newChannelId)) {
                // Add to local list
                const newChannel = {
                    id: newChannelId,
                    name: response.name,
                    type: 'group',
                    members: (selectedMemberIds.length + 1) // +1 for creator
                };
                channels.push(newChannel);
                // Refresh sidebar
                renderSidebar();
            }

            // Close modal
            toggleCreateChannelModal();

            // Open new channel
            loadChat(newChannelId, 'group');
        },
        error: function (xhr) {
            console.error("Failed to create channel", xhr);
            alert("Failed to create channel");
        }
    });
}

// Populate Sidebar initially
renderSidebar();

// Auto-load general chat
// loadChat('general', 'group');

// --- Search Functionality ---
const searchInput = document.getElementById('search-input');
const searchResults = document.getElementById('search-results');
let searchTimeout;

searchInput.addEventListener('input', (e) => {
    const query = e.target.value.trim();
    clearTimeout(searchTimeout);

    if (query.length < 1) {
        searchResults.classList.add('hidden');
        return;
    }

    searchTimeout = setTimeout(() => {
        performSearch(query);
    }, 300); // Debounce 300ms
});

// Hide search results when clicking outside
document.addEventListener('click', (e) => {
    if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
        searchResults.classList.add('hidden');
    }
});

function performSearch(query) {
    // Parallel fetch for users and channels
    Promise.all([
        fetch(`/api/v1/users/search?query=${encodeURIComponent(query)}`).then(res => res.json()),
        fetch(`/api/chat/channels/search?query=${encodeURIComponent(query)}`).then(res => res.json())
    ]).then(([foundUsers, foundChannels]) => {
        renderSearchResults(foundUsers, foundChannels);
    }).catch(err => {
        console.error("Search failed:", err);
    });
}

function renderSearchResults(foundUsers, foundChannels) {
    if (foundUsers.length === 0 && foundChannels.length === 0) {
        searchResults.innerHTML = `<div class="p-3 text-sm text-gray-500 text-center">No results found</div>`;
        searchResults.classList.remove('hidden');
        return;
    }

    let html = '';

    // Users Section
    if (foundUsers.length > 0) {
        html += `<div class="px-3 py-2 text-xs font-semibold text-gray-500 uppercase bg-gray-50">People</div>`;
        html += foundUsers.map(u => `
            <div onclick="selectSearchResult('${u.id}', '${u.name || u.username}', '${u.picture || ''}', 'dm')" 
                 class="flex items-center gap-3 px-3 py-2 hover:bg-blue-50 cursor-pointer transition-colors">
                <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden text-xs font-bold text-gray-600">
                    ${u.picture ? `<img src="${u.picture}" class="w-full h-full object-cover">` : (u.name || u.username).substring(0, 2).toUpperCase()}
                </div>
                <div class="text-sm font-medium text-gray-900">${u.name || u.username}</div>
            </div>
        `).join('');
    }

    // Channels Section
    if (foundChannels.length > 0) {
        html += `<div class="px-3 py-2 text-xs font-semibold text-gray-500 uppercase bg-gray-50 border-t border-gray-100">Channels</div>`;
        html += foundChannels.map(c => `
            <div onclick="selectSearchResult('${c.id}', '${c.name}', '', 'group')" 
                 class="flex items-center gap-3 px-3 py-2 hover:bg-blue-50 cursor-pointer transition-colors">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-xs font-bold">
                    #
                </div>
                <div class="text-sm font-medium text-gray-900">${c.name}</div>
            </div>
        `).join('');
    }

    searchResults.innerHTML = html;
    searchResults.classList.remove('hidden');
}

function selectSearchResult(id, name, avatar, type) {
    // Hide results and clear input
    searchResults.classList.add('hidden');
    searchInput.value = '';

    if (type === 'dm') {
        // Check if user exists in local list
        let user = users.find(u => u.id === String(id));
        if (!user) {
            // Add to local list if not exists
            user = {
                id: String(id),
                name: name,
                avatar: avatar ? `<img src="${avatar}" class="w-full h-full object-cover">` : name.substring(0, 2).toUpperCase(),
                color: 'bg-gray-100 text-gray-700', // Default color
                status: 'offline', // Default status
                type: 'dm'
            };
            users.push(user);
        }
        // Load chat
        loadChat(String(id), 'dm');
    } else {
        // Check if channel exists in local list
        let channel = channels.find(c => c.id === String(id));
        if (!channel) {
            channel = {
                id: String(id),
                name: name,
                type: 'group',
                members: 0 // We might need to fetch this
            };
            channels.push(channel);
        }
        loadChat(String(id), 'group');
    }
}

function leaveGroup(channelId) {
    if (!confirm("Are you sure you want to leave this group?")) return;

    $.ajax({
        type: 'POST',
        url: `/api/chat/channel/${channelId}/leave`,
        success: function (response) {
            // UI update will happen via WebSocket notification or we can do it optimistically
            // But since we have the notification logic, let's rely on that or force it.
            // For better UX, let's remove it immediately.
            channels = channels.filter(c => c.id !== String(channelId));
            renderSidebar();
            if (activeChatId === String(channelId)) {
                activeChatId = null;
                messagesContainer.innerHTML = '<div class="flex justify-center items-center h-full text-gray-500">Select a chat to start messaging</div>';
                chatInfoEl.innerHTML = '';
                addMemberBtn.style.display = 'none';
                document.getElementById('group-actions-btn')?.remove();
            }
        },
        error: function (xhr) {
            console.error("Failed to leave group", xhr);
            alert("Failed to leave group");
        }
    });
}

function deleteGroup(channelId) {
    if (!confirm("Are you sure you want to delete this group? This cannot be undone.")) return;

    $.ajax({
        type: 'DELETE',
        url: `/api/chat/channel/${channelId}`,
        success: function (response) {
            // UI update via WebSocket
        },
        error: function (xhr) {
            console.error("Failed to delete group", xhr);
            alert("Failed to delete group (Only creator can delete)");
        }
    });
}

