<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* Global styles can remain here */
body {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  background-color: #f4f7f9;
  margin: 0;
  padding: 2em;
  display: flex;
  justify-content: center;
  align-items: flex-start; /* Align to the top */
  min-height: 100vh;
}
</style>
