// ========== SISTEMA DE TOAST NOTIFICATIONS ==========
const Toast = {
    container: null,

    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },

    show(message, type = 'success', duration = 3000) {
        this.init();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const icon = this.getIcon(type);

        toast.innerHTML = `
            <span class="toast-icon">${icon}</span>
            <span class="toast-message">${message}</span>
            <button class="toast-close">×</button>
        `;

        this.container.appendChild(toast);

        // Botón cerrar
        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('click', () => this.remove(toast));

        // Auto cerrar
        setTimeout(() => this.remove(toast), duration);
    },

    remove(toast) {
        toast.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    },

    getIcon(type) {
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };
        return icons[type] || 'ℹ️';
    }
};

// ========== SISTEMA DE MODAL ==========
const Modal = {
    show({ title, message, type = 'info', onConfirm = null }) {
        // Remover modal existente
        const existing = document.querySelector('.modal-overlay');
        if (existing) existing.remove();

        const icons = {
            success: { emoji: '✅', bg: '#D1FAE5' },
            error: { emoji: '❌', bg: '#FEE2E2' },
            warning: { emoji: '⚠️', bg: '#FEF3C7' },
            info: { emoji: 'ℹ️', bg: '#DBEAFE' }
        };

        const iconData = icons[type] || icons.info;

        const modalHtml = `
            <div class="modal-overlay active">
                <div class="modal">
                    <div class="modal-header">
                        <div class="modal-header-icon ${type}" style="background: ${iconData.bg}">
                            ${iconData.emoji}
                        </div>
                        <h3>${title}</h3>
                    </div>
                    <div class="modal-body">
                        ${message}
                    </div>
                    <div class="modal-footer">
                        <button class="modal-btn modal-btn-primary" id="modalConfirmBtn">Aceptar</button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);

        const modalOverlay = document.querySelector('.modal-overlay');
        const confirmBtn = document.getElementById('modalConfirmBtn');

        const closeModal = () => {
            modalOverlay.classList.remove('active');
            setTimeout(() => modalOverlay.remove(), 300);
        };

        confirmBtn.addEventListener('click', () => {
            closeModal();
            if (onConfirm) onConfirm();
        });

        // Cerrar al hacer clic fuera
        modalOverlay.addEventListener('click', (e) => {
            if (e.target === modalOverlay) closeModal();
        });
    }
};

// ========== VALIDACIÓN EN TIEMPO REAL ==========
class FormValidator {
    constructor(formId, rules) {
        this.form = document.getElementById(formId);
        this.rules = rules;
        this.init();
    }

    init() {
        if (!this.form) return;

        for (const [fieldName, rule] of Object.entries(this.rules)) {
            const input = this.form.querySelector(`[name="${fieldName}"]`);
            if (input) {
                input.addEventListener('input', () => this.validateField(input, rule));
                input.addEventListener('blur', () => this.validateField(input, rule));
            }
        }
    }

    validateField(input, rule) {
        const value = input.value;
        let isValid = true;
        let message = '';

        if (rule.required && !value.trim()) {
            isValid = false;
            message = rule.requiredMessage || 'Este campo es obligatorio';
        } else if (rule.minLength && value.length < rule.minLength) {
            isValid = false;
            message = `Mínimo ${rule.minLength} caracteres`;
        } else if (rule.maxLength && value.length > rule.maxLength) {
            isValid = false;
            message = `Máximo ${rule.maxLength} caracteres`;
        } else if (rule.pattern && !rule.pattern.test(value)) {
            isValid = false;
            message = rule.patternMessage || 'Formato inválido';
        } else if (rule.matchWith) {
            const matchField = this.form.querySelector(`[name="${rule.matchWith}"]`);
            if (matchField && value !== matchField.value) {
                isValid = false;
                message = 'Las contraseñas no coinciden';
            }
        }

        // Aplicar clases visuales
        input.classList.remove('valid', 'invalid');
        input.classList.add(isValid ? 'valid' : 'invalid');

        // Mostrar/remover mensaje de error
        let messageSpan = input.parentElement.querySelector('.validation-message');
        if (!messageSpan && !isValid) {
            messageSpan = document.createElement('div');
            messageSpan.className = 'validation-message';
            input.parentElement.appendChild(messageSpan);
        }

        if (messageSpan) {
            if (isValid && rule.showSuccess) {
                messageSpan.className = 'validation-message success';
                messageSpan.innerHTML = `<span class="validation-icon">✓</span> Correcto`;
            } else if (!isValid) {
                messageSpan.className = 'validation-message error';
                messageSpan.innerHTML = `<span class="validation-icon">⚠️</span> ${message}`;
            } else {
                messageSpan.innerHTML = '';
            }
        }

        return isValid;
    }

    validateAll() {
        let allValid = true;
        for (const [fieldName, rule] of Object.entries(this.rules)) {
            const input = this.form.querySelector(`[name="${fieldName}"]`);
            if (input) {
                const isValid = this.validateField(input, rule);
                if (!isValid) allValid = false;
            }
        }
        return allValid;
    }
}

// ========== PREVENIR ENVÍO MÚLTIPLE DE FORMULARIOS ==========
function initFormSubmitProtection() {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.classList.contains('btn-loading')) {
                submitBtn.classList.add('btn-loading');
                submitBtn.disabled = true;
            }
        });
    });
}

// ========== INICIALIZAR TODO AL CARGAR ==========
document.addEventListener('DOMContentLoaded', () => {
    initFormSubmitProtection();

    // Mostrar mensajes de error desde el backend (si existen)
    const errorElement = document.querySelector('#error-message');
    if (errorElement && errorElement.value) {
        Toast.show(errorElement.value, 'error', 5000);
    }

    const successElement = document.querySelector('#success-message');
    if (successElement && successElement.value) {
        Toast.show(successElement.value, 'success', 3000);
    }
});