# NagiHealth

Личное приложение для похудения и формы: тренировки с гирей с таймером и анимацией, фейсбилдинг, уход, еда, вода, мотивация, виджет и напоминания со своими мелодиями.

- Код: Android (Java + WebView). Интерфейс: `app/src/main/assets/index.html`.
- Сборка: GitHub Actions при каждом push в `main` → релиз `vN` с `NagiHealth.apk`.
- Приложение само проверяет релизы и предлагает обновиться.
- `config.json` — программа, карточки дня, миссии, напоминания. Меняется без пересборки (приложение подтягивает его при запуске).

## Поля config.json
`msg`, `cfg{kcal,protein,water,goal,start,glass}`, `schedule`, `cards[{date|dow,emoji,title,text,task{label,action}}]`,
`missions[{label,rew,dow}]`, `reminders[{t,type,text,dow,act}]` (type: water|meal|move|train|care|weigh|face|alarm),
`ex`, `rew`, `wrew`, `workouts`, `dayw`, `xpv`, `care`, `remove`, `tips`.
