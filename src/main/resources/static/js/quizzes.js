const questionFields = document.querySelector("#question-fields");
const addQuestionButton = document.querySelector("#add-question");

addQuestionButton?.addEventListener("click", () => {
    const count = questionFields.querySelectorAll(".question-editor").length;
    if (count >= 20) {
        addQuestionButton.disabled = true;
        return;
    }
    const fieldset = questionFields.querySelector(".question-editor").cloneNode(true);
    fieldset.querySelector("legend").textContent = `Question ${count + 1}`;
    fieldset.querySelectorAll("input").forEach((input) => {
        input.value = "";
        input.required = true;
    });
    fieldset.querySelector("select").selectedIndex = 0;
    questionFields.append(fieldset);
    if (count + 1 >= 20) addQuestionButton.disabled = true;
});
