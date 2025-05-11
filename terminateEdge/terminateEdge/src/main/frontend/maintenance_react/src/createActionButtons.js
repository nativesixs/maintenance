export function createButton(id, clickHandler) {
    const button = document.createElement('input');
    const name = id;
    button.setAttribute('type', 'submit');
    button.setAttribute('id', id);
    button.setAttribute('name', name);
    button.setAttribute('value', 'Provest Akci');
    button.setAttribute('class', 'submit');
    button.setAttribute('style', 'width: 175px');
    button.onclick = clickHandler;
    return button;
}